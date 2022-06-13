package com.github.mkorman9.vertx.security

import at.favre.lib.crypto.bcrypt.BCrypt
import com.github.mkorman9.vertx.utils.AsyncRoot
import com.github.mkorman9.vertx.utils.SecureRandomGenerator
import com.github.mkorman9.vertx.utils.web.*
import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.await
import java.time.LocalDateTime

@Singleton
class SessionApi @Inject constructor(
    private val vertx: Vertx,
    private val accountRepository: AccountRepository,
    private val sessionRepository: SessionRepository,
    private val authorizationMiddleware: AuthorizationMiddleware
) : AsyncRoot(vertx) {
    private val sessionIdLength: Long = 24
    private val sessionTokenLength: Long = 48
    private val sessionDurationSeconds: Int = 4 * 60 * 60

    private val bcryptVerifier: BCrypt.Verifyer = BCrypt.verifyer()

    fun createRouter(): Router = Router.router(vertx).apply {
        post("/")
            .asyncHandler(scope) { ctx ->
                ctx.handleJsonBody<StartSessionPayload> { payload ->
                    val account = accountRepository.findByCredentialsEmail(payload.email).await()
                    if (account == null) {
                        ctx.response().setStatusCode(401).endWithJson(
                            StatusDTO(
                            status = "error",
                            message = "invalid credentials",
                            causes = listOf(Cause("credentials", "invalid"))
                        )
                        )

                        return@handleJsonBody
                    }

                    if (!account.active) {
                        ctx.response().setStatusCode(401).endWithJson(
                            StatusDTO(
                            status = "error",
                            message = "account is not active",
                            causes = listOf(Cause("account", "inactive"))
                        )
                        )

                        return@handleJsonBody
                    }

                    val verificationResult = vertx.executeBlocking<BCrypt.Result> { call ->
                        call.complete(
                            bcryptVerifier.verify(payload.password.toCharArray(), account.credentials!!.passwordBcrypt)
                        )
                    }.await()

                    if (!verificationResult.verified) {
                        ctx.response().setStatusCode(401).endWithJson(
                            StatusDTO(
                            status = "error",
                            message = "invalid credentials",
                            causes = listOf(Cause("credentials", "invalid"))
                        )
                        )

                        return@handleJsonBody
                    }

                    val session = Session(
                        id = SecureRandomGenerator.generate(sessionIdLength),
                        accountId = account.id,
                        token = SecureRandomGenerator.generate(sessionTokenLength),
                        rolesString = account.rolesString,
                        ip = ctx.request().getClientIp(),
                        issuedAt = LocalDateTime.now(),
                        duration = sessionDurationSeconds,
                        expiresAt = LocalDateTime.now().plusSeconds(sessionDurationSeconds.toLong()),
                        account = account
                    )

                    val newSession = sessionRepository.add(session).await()
                    ctx.response().endWithJson(newSession)
                }
            }

        put("/")
            .handler { ctx -> authorizationMiddleware.authorize(ctx) }
            .asyncHandler(scope) { ctx ->
                val session = authorizationMiddleware.getActiveSession(ctx)
                val refreshedSession = sessionRepository.refresh(session).await()
                ctx.response().endWithJson(refreshedSession)
            }

        delete("/")
            .handler { ctx -> authorizationMiddleware.authorize(ctx) }
            .asyncHandler(scope) { ctx ->
                val session = authorizationMiddleware.getActiveSession(ctx)
                sessionRepository.delete(session).await()
                ctx.response().endWithJson(StatusDTO(status = "ok"))
            }
    }
}

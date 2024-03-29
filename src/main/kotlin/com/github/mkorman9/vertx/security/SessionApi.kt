package com.github.mkorman9.vertx.security

import at.favre.lib.crypto.bcrypt.BCrypt
import com.github.mkorman9.vertx.common.Services
import com.github.mkorman9.vertx.utils.SecureRandomGenerator
import com.github.mkorman9.vertx.utils.core.VerticleContext
import com.github.mkorman9.vertx.utils.web.*
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.await
import java.time.LocalDateTime

class SessionApi (services: Services, context: VerticleContext) {
    private val accountRepository: AccountRepository = services.accountRepository
    private val sessionRepository: SessionRepository = services.sessionRepository
    private val authorizationMiddleware: AuthorizationMiddleware = services.authorizationMiddleware

    private val sessionTokenLength: Long = 64
    private val sessionDurationSeconds: Int = 4 * 60 * 60

    private val bcryptVerifier: BCrypt.Verifyer = BCrypt.verifyer()

    val router: Router = Router.router(context.vertx).apply {
        post("/")
            .coroutineHandler(context.scope) { ctx ->
                ctx.handleJsonBody(StartSessionPayload::class.java) { payload ->
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

                    val verificationResult = context.vertx.executeBlocking<BCrypt.Result> { call ->
                        call.complete(
                            bcryptVerifier.verify(payload.password.toCharArray(), account.credentials.passwordBcrypt)
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
                        token = SecureRandomGenerator.generate(sessionTokenLength),
                        roles = account.roles,
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
            .coroutineHandler(context.scope) { ctx ->
                val session = authorizationMiddleware.getActiveSession(ctx)
                val refreshedSession = sessionRepository.refresh(session).await()
                ctx.response().endWithJson(refreshedSession)
            }

        delete("/")
            .handler { ctx -> authorizationMiddleware.authorize(ctx) }
            .coroutineHandler(context.scope) { ctx ->
                val session = authorizationMiddleware.getActiveSession(ctx)
                sessionRepository.delete(session).await()
                ctx.response().endWithJson(StatusDTO(status = "ok"))
            }
    }
}

package com.github.mkorman9.vertx.security

import at.favre.lib.crypto.bcrypt.BCrypt
import com.github.mkorman9.vertx.utils.SecureRandomGenerator
import com.github.mkorman9.vertx.utils.VerticleContext
import com.github.mkorman9.vertx.utils.web.*
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.await
import java.time.LocalDateTime

class SessionApi (context: VerticleContext) {
    private val accountRepository: AccountRepository = context.injector.getInstance()
    private val sessionRepository: SessionRepository = context.injector.getInstance()
    private val authorizationMiddleware: AuthorizationMiddleware = context.injector.getInstance()

    private val sessionTokenLength: Long = 48
    private val sessionDurationSeconds: Int = 4 * 60 * 60

    private val bcryptVerifier: BCrypt.Verifyer = BCrypt.verifyer()

    val router: Router = Router.router(context.vertx).apply {
        post("/")
            .coroutineHandler(context.scope) { ctx ->
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

                    val verificationResult = context.vertx.executeBlocking<BCrypt.Result> { call ->
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
                        accountId = account.id,
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

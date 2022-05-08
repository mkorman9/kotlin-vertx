package com.github.mkorman9.vertx.security

import at.favre.lib.crypto.bcrypt.BCrypt
import com.github.mkorman9.vertx.AppContext
import com.github.mkorman9.vertx.utils.*
import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.ext.web.Router
import java.time.LocalDateTime

@Singleton
class SessionApi @Inject constructor(
    private val context: AppContext,
    private val accountRepository: AccountRepository,
    private val sessionRepository: SessionRepository,
    private val authorizationMiddleware: AuthorizationMiddleware
) {
    private val sessionIdLength: Long = 24
    private val sessionTokenLength: Long = 48
    private val sessionDurationSeconds: Int = 4 * 60 * 60

    private val bcryptVerifier: BCrypt.Verifyer = BCrypt.verifyer()

    val router: Router = Router.router(context.vertx).apply {
        post("/").handler { ctx ->
            ctx.handleJsonBody<StartSessionPayload> { payload ->
                accountRepository.findByCredentialsEmail(payload.email)
                    .onSuccess { account ->
                        if (account == null) {
                            ctx.response().setStatusCode(401).endWithJson(StatusDTO(
                                status = "error",
                                message = "invalid credentials",
                                causes = listOf(Cause("credentials", "invalid"))
                            ))

                            return@onSuccess
                        }

                        if (!account.active) {
                            ctx.response().setStatusCode(401).endWithJson(StatusDTO(
                                status = "error",
                                message = "account is not active",
                                causes = listOf(Cause("account", "inactive"))
                            ))

                            return@onSuccess
                        }

                        context.vertx.executeBlocking<BCrypt.Result> { call ->
                            call.complete(
                                bcryptVerifier.verify(payload.password.toCharArray(), account.credentials.passwordBcrypt)
                            )
                        }
                            .onSuccess { result ->
                                if (!result.verified) {
                                    ctx.response().setStatusCode(401).endWithJson(StatusDTO(
                                            status = "error",
                                            message = "invalid credentials",
                                            causes = listOf(Cause("credentials", "invalid"))
                                    ))

                                    return@onSuccess
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

                                sessionRepository.add(session)
                                    .onSuccess { ctx.response().endWithJson(it) }
                                    .onFailure { failure -> ctx.fail(500, failure) }
                            }
                            .onFailure { failure -> ctx.fail(500, failure) }
                    }
                    .onFailure { failure -> ctx.fail(500, failure) }
            }
        }

        put("/")
            .handler { ctx -> authorizationMiddleware.authorize(ctx) }
            .handler { ctx ->
                val session = authorizationMiddleware.getActiveSession(ctx)
                sessionRepository.refresh(session)
                    .onSuccess { refreshedSession ->
                        ctx.response().endWithJson(refreshedSession)
                    }
                    .onFailure { failure -> ctx.fail(500, failure) }
            }

        delete("/")
            .handler { ctx -> authorizationMiddleware.authorize(ctx) }
            .handler { ctx ->
                val session = authorizationMiddleware.getActiveSession(ctx)
                sessionRepository.delete(session)
                    .onSuccess { deleted ->
                        if (deleted) {
                            ctx.response().endWithJson(StatusDTO(status = "ok"))
                        } else {
                            ctx.response().setStatusCode(500).endWithJson(StatusDTO(
                                status = "error",
                                message = "failed to revoke session"
                            ))
                        }
                    }
                    .onFailure { failure -> ctx.fail(500, failure) }
            }
    }
}

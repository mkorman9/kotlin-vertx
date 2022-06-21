package com.github.mkorman9.vertx.security

import at.favre.lib.crypto.bcrypt.BCrypt
import com.github.mkorman9.vertx.tools.hibernate.isUniqueConstraintViolation
import com.github.mkorman9.vertx.utils.SecureRandomGenerator
import com.github.mkorman9.vertx.utils.VerticleContext
import com.github.mkorman9.vertx.utils.web.*
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.await
import java.time.LocalDateTime
import java.util.*

class AccountApi (context: VerticleContext) {
    private val accountRepository: AccountRepository = context.injector.getInstance()

    private val bcryptHasher: BCrypt.Hasher = BCrypt.with(SecureRandomGenerator.INSTANCE)

    val router: Router = Router.router(context.vertx).apply {
        post("/")
            .coroutineHandler(context.scope) { ctx ->
                ctx.handleJsonBody<AccountAddPayload> { payload ->
                    val passwordBcrypt = context.vertx.executeBlocking<String> { call ->
                        val result = bcryptHasher.hashToString(12, payload.password.toCharArray())
                        call.complete(result)
                    }.await()

                    val account = Account(
                        id = UUID.randomUUID(),
                        username = payload.username,
                        roles = mutableSetOf("CLIENTS_EDITOR"),
                        active = true,
                        deleted = false,
                        preferredLanguage = payload.language,
                        bannedUntil = null,
                        createdAt = LocalDateTime.now(),
                        credentials = AccountCredentials(
                            email = payload.email,
                            passwordBcrypt = passwordBcrypt,
                            lastChangeAt = LocalDateTime.now(),
                            lastChangeIp = ctx.request().getClientIp()
                        )
                    )

                    try {
                        accountRepository.add(account).await()

                        ctx.response().endWithJson(
                            StatusDTO(
                                status = "ok"
                            )
                        )
                    } catch(e: Exception) {
                        if (isUniqueConstraintViolation(e, "unique_accounts_username")) {
                            ctx.response().setStatusCode(400).endWithJson(
                                StatusDTO(
                                    status = "error",
                                    causes = listOf(Cause("username", "unique"))
                                )
                            )
                        } else if (isUniqueConstraintViolation(e, "unique_accounts_credentials_email")) {
                            ctx.response().setStatusCode(400).endWithJson(
                                StatusDTO(
                                    status = "error",
                                    causes = listOf(Cause("email", "unique"))
                                )
                            )
                        } else {
                            throw e
                        }
                    }
                }
            }
    }
}

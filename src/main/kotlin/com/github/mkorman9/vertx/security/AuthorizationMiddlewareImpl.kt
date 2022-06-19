package com.github.mkorman9.vertx.security

import com.github.mkorman9.vertx.utils.web.StatusDTO
import com.github.mkorman9.vertx.utils.web.endWithJson
import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.RoutingContext

@Singleton
class AuthorizationMiddlewareImpl @Inject constructor(
    private val sessionRepository: SessionRepository
) : AuthorizationMiddleware {
    private val sessionObjectKey = "activeSession"

    override fun authorize(ctx: RoutingContext, allowedRoles: Set<String>?) {
        ctx.request().pause()

        val token = retrieveBearerToken(ctx.request())
        if (token == null) {
            ctx.response().setStatusCode(401).endWithJson(
                StatusDTO(
                    status = "error",
                    message = "Authorization required"
                )
            )

            return
        }

        sessionRepository.findByToken(ctx.vertx(), token)
            .onSuccess { session ->
                if (session == null) {
                    ctx.response().setStatusCode(401).endWithJson(
                        StatusDTO(
                            status = "error",
                            message = "Authorization failed"
                        )
                    )

                    return@onSuccess
                }

                if (allowedRoles != null && !verifyRoles(session.roles, allowedRoles)) {
                    ctx.response().setStatusCode(403).endWithJson(
                        StatusDTO(
                            status = "error",
                            message = "Access denied"
                        )
                    )

                    return@onSuccess
                }

                ctx.put(sessionObjectKey, session)
                ctx.request().resume()
                ctx.next()
            }
            .onFailure { failure -> ctx.fail(500, failure) }
    }

    override fun getActiveSession(ctx: RoutingContext): Session {
        return ctx.get(sessionObjectKey)
    }

    private fun retrieveBearerToken(request: HttpServerRequest): String? {
        val headerValue = request.getHeader("Authorization") ?: return null

        val parts = headerValue.split(" ")
        if (parts.size != 2) {
            return null
        }

        if (parts[0].lowercase() != "bearer") {
            return null
        }

        return parts[1]
    }

    private fun verifyRoles(providedRoles: Set<String>, allowedRoles: Set<String>): Boolean {
        return providedRoles.any { role -> allowedRoles.contains(role) }
    }
}

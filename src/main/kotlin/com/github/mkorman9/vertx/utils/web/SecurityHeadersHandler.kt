package com.github.mkorman9.vertx.utils.web

import com.github.mkorman9.vertx.utils.Config
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import com.github.mkorman9.vertx.utils.get

object SecurityHeadersHandler {
    fun create(config: Config): Handler<RoutingContext> {
        val isEnabled = config.get<Boolean>("server.security.headers") ?: false

        return Handler { ctx ->
            if (isEnabled) {
                ctx.response().putHeader("X-Frame-Options", "DENY")
                ctx.response().putHeader("X-Content-Type-Options", "nosniff")
                ctx.response().putHeader("X-XSS-Protection", "0")

                if (ctx.request().isClientTLS()) {
                    ctx.response().putHeader(
                        "Strict-Transport-Security",
                        "max-age=63072000; includeSubDomains; preload"
                    )
                }
            }

            ctx.next()
        }
    }
}

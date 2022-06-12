package com.github.mkorman9.vertx.utils.web

import com.github.mkorman9.vertx.utils.DeploymentContext
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext

class HealthcheckHandler {
    companion object {
        private val context = DeploymentContext.create()

        fun create(): Handler<RoutingContext> = Handler { ctx ->
            ctx.response().endWithJson(
                HealthcheckResponse(
                    status = "healthy",
                    version = context.version,
                    startupTime = context.startupTime,
                    environment = context.environment
                )
            )
        }
    }
}

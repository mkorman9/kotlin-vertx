package com.github.mkorman9.vertx.utils.web

import com.github.mkorman9.vertx.utils.DeploymentInfo
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext

object HealthcheckHandler {
    private val info = DeploymentInfo.get()

    fun create(): Handler<RoutingContext> = Handler { ctx ->
        ctx.response().endWithJson(
            HealthcheckResponse(
                status = "healthy",
                version = info.version,
                startupTime = info.startupTime,
                environment = info.environment,
                profile = info.profile
            )
        )
    }
}

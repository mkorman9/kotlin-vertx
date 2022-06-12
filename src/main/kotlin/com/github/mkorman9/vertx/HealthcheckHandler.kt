package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.utils.DeploymentContext
import com.github.mkorman9.vertx.utils.web.HealthcheckResponse
import com.github.mkorman9.vertx.utils.web.endWithJson
import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext

@Singleton
class HealthcheckHandler @Inject constructor(
    private val context: DeploymentContext
) {
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

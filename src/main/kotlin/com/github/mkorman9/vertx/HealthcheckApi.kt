package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.utils.endWithJson
import io.vertx.ext.web.Router

class HealthcheckApi(
    private val context: AppContext
) {
    val router: Router = Router.router(context.vertx).apply {
        get("/").handler { ctx ->
            ctx.response().endWithJson(HealthcheckResponse(
                status = "healthy",
                version = context.version,
                startupTime = context.startupTime
            ))
        }
    }
}

package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.utils.endWithJson
import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.ext.web.Router

@Singleton
class HealthcheckApi @Inject constructor(
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

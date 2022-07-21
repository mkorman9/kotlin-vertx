package com.github.mkorman9.vertx.appinfo

import com.github.mkorman9.vertx.utils.DeploymentInfo
import com.github.mkorman9.vertx.utils.VerticleContext
import com.github.mkorman9.vertx.utils.web.coroutineHandler
import com.github.mkorman9.vertx.utils.web.endWithJson
import io.vertx.ext.web.Router

class AppInfoApi (context: VerticleContext) {
    private val info = DeploymentInfo.get()

    val router: Router = Router.router(context.vertx).apply {
        get("/")
            .coroutineHandler(context.scope) { ctx ->
                ctx.response().endWithJson(
                    AppInfoResponse(
                        envName = info.environment,
                        profile = info.profile
                    )
                )
            }
    }
}

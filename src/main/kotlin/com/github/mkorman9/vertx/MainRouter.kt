package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientRouter
import com.github.mkorman9.vertx.utils.StatusDTO
import com.github.mkorman9.vertx.utils.endWithJson
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.web.Router

class MainRouter(
    private val context: AppContext
) {
    private val log = LoggerFactory.getLogger(MainRouter::class.java)

    val router: Router = Router.router(context.vertx).apply {
        mountSubRouter("/health", HealthcheckRouter(context).router)

        mountSubRouter("/api/v1/client", ClientRouter(context).router)

        errorHandler(404) { ctx ->
            ctx.response().endWithJson(StatusDTO(
                status = "error",
                message = "not found"
            ))
        }

        errorHandler(500) { ctx ->
            log.error("Unexpected error while serving request", ctx.failure())

            ctx.response().endWithJson(StatusDTO(
                status = "error",
                message = "internal server error"
            ))
        }
    }
}

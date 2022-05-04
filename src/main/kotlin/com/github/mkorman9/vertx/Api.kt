package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientApi
import com.github.mkorman9.vertx.security.SessionApi
import com.github.mkorman9.vertx.utils.StatusDTO
import com.github.mkorman9.vertx.utils.endWithJson
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.micrometer.PrometheusScrapingHandler

class Api(
    private val context: AppContext
) {
    private val log = LoggerFactory.getLogger(Api::class.java)

    val router: Router = Router.router(context.vertx).apply {
        mountSubRouter("/health", HealthcheckApi(context).router)
        route("/metrics").handler(PrometheusScrapingHandler.create())

        mountSubRouter("/api/v1/client", ClientApi(context).router)

        mountSubRouter("/api/v1/session", SessionApi(context).router)

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

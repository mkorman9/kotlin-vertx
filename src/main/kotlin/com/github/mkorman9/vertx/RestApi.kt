package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientApi
import com.github.mkorman9.vertx.security.SessionApi
import com.github.mkorman9.vertx.utils.web.HealthcheckHandler
import com.github.mkorman9.vertx.utils.web.StatusDTO
import com.github.mkorman9.vertx.utils.web.endWithJson
import com.google.inject.Injector
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.micrometer.PrometheusScrapingHandler
import kotlinx.coroutines.CoroutineScope

class RestApi (
    vertx: Vertx,
    scope: CoroutineScope,
    injector: Injector
) {
    companion object {
        private val log = LoggerFactory.getLogger(RestApi::class.java)
    }

    val router: Router = Router.router(vertx).apply {
        route().handler(BodyHandler.create())

        route("/health").handler(HealthcheckHandler.create())
        route("/metrics").handler(PrometheusScrapingHandler.create())

        route("/api/v1/client*").subRouter(ClientApi(vertx, scope, injector).router)
        route("/api/v1/session*").subRouter(SessionApi(vertx, scope, injector).router)

        errorHandler(404) { ctx ->
            ctx.response().endWithJson(
                StatusDTO(
                    status = "error",
                    message = "not found"
                )
            )
        }

        errorHandler(500) { ctx ->
            log.error("Unexpected error while serving request", ctx.failure())

            ctx.response().endWithJson(
                StatusDTO(
                    status = "error",
                    message = "internal server error"
                )
            )
        }
    }
}

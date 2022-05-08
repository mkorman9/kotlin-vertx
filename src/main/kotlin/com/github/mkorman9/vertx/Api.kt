package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientApi
import com.github.mkorman9.vertx.security.SessionApi
import com.github.mkorman9.vertx.utils.StatusDTO
import com.github.mkorman9.vertx.utils.endWithJson
import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.micrometer.PrometheusScrapingHandler

@Singleton
class Api @Inject constructor(
    private val vertx: Vertx,
    private val healthcheckApi: HealthcheckApi,
    private val clientApi: ClientApi,
    private val sessionApi: SessionApi
) {
    private val log = LoggerFactory.getLogger(Api::class.java)

    val router: Router = Router.router(vertx).apply {
        mountSubRouter("/health", healthcheckApi.router)
        route("/metrics").handler(PrometheusScrapingHandler.create())

        mountSubRouter("/api/v1/client", clientApi.router)

        mountSubRouter("/api/v1/session", sessionApi.router)

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

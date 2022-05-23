package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientApi
import com.github.mkorman9.vertx.security.SessionApi
import com.github.mkorman9.vertx.utils.StatusDTO
import com.github.mkorman9.vertx.utils.endWithJson
import com.google.inject.Injector
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.config.ConfigRetriever
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.micrometer.PrometheusScrapingHandler

class Api (injector: Injector) {
    private val log = LoggerFactory.getLogger(Api::class.java)

    private val vertx = injector.getInstance<Vertx>()
    private val config = injector.getInstance<ConfigRetriever>().cachedConfig

    private val healthcheckHandler = injector.getInstance<HealthcheckHandler>()
    private val clientApi = injector.getInstance<ClientApi>()
    private val sessionApi = injector.getInstance<SessionApi>()

    private val healthcheckPath = config
        .getJsonObject("server")
        ?.getJsonObject("endpoints")
        ?.getString("health")
        ?: "/health"
    private val metricsPath = config
        .getJsonObject("server")
        ?.getJsonObject("endpoints")
        ?.getString("metrics")
        ?: "/metrics"

    fun createRouter(): Router = Router.router(vertx).apply {
        route(healthcheckPath).handler(healthcheckHandler.create())
        route(metricsPath).handler(PrometheusScrapingHandler.create())

        route("/api/v1/client*").subRouter(clientApi.createRouter())
        route("/api/v1/session*").subRouter(sessionApi.createRouter())

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

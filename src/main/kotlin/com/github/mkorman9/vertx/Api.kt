package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientApi
import com.github.mkorman9.vertx.security.SessionApi
import com.github.mkorman9.vertx.utils.HealthcheckHandler
import com.github.mkorman9.vertx.utils.web.StatusDTO
import com.github.mkorman9.vertx.utils.web.endWithJson
import com.google.inject.Injector
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.micrometer.PrometheusScrapingHandler

class Api (injector: Injector) {
    companion object {
        private val log = LoggerFactory.getLogger(Api::class.java)
    }

    private val vertx = injector.getInstance<Vertx>()

    private val clientApi = injector.getInstance<ClientApi>()
    private val sessionApi = injector.getInstance<SessionApi>()

    fun createRouter(): Router = Router.router(vertx).apply {
        route().handler(BodyHandler.create())

        route("/health").handler(HealthcheckHandler.create())
        route("/metrics").handler(PrometheusScrapingHandler.create())

        route("/api/v1/client*").subRouter(clientApi.createRouter())
        route("/api/v1/session*").subRouter(sessionApi.createRouter())

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

package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.appinfo.AppInfoApi
import com.github.mkorman9.vertx.client.ClientApi
import com.github.mkorman9.vertx.common.Services
import com.github.mkorman9.vertx.security.AccountApi
import com.github.mkorman9.vertx.security.SessionApi
import com.github.mkorman9.vertx.utils.core.VerticleContext
import com.github.mkorman9.vertx.utils.web.HealthcheckHandler
import com.github.mkorman9.vertx.utils.web.SecurityHeadersHandler
import com.github.mkorman9.vertx.utils.web.StatusDTO
import com.github.mkorman9.vertx.utils.web.endWithJson
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.micrometer.PrometheusScrapingHandler

class RestApi (services: Services, context: VerticleContext) {
    companion object {
        private val log = LoggerFactory.getLogger(RestApi::class.java)
    }

    val router: Router = Router.router(context.vertx).apply {
        route().handler(BodyHandler.create().setHandleFileUploads(false))
        route().handler(SecurityHeadersHandler.create(context.config))
        
        route("/health").handler(HealthcheckHandler.create())
        route("/metrics").handler(PrometheusScrapingHandler.create())

        route("/api/v1/info*").subRouter(AppInfoApi(context).router)
        route("/api/v1/client*").subRouter(ClientApi(services, context).router)
        route("/api/v1/session*").subRouter(SessionApi(services, context).router)
        route("/api/v1/account*").subRouter(AccountApi(services, context).router)

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

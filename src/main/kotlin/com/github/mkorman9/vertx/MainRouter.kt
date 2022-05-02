package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientRepository
import com.github.mkorman9.vertx.client.createClientRouter
import com.github.mkorman9.vertx.utils.StatusDTO
import com.github.mkorman9.vertx.utils.endWithJson
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.web.Router

class MainRouter(
    private val context: AppContext
) {
    private val log = LoggerFactory.getLogger(MainRouter::class.java)

    private val clientRepository = ClientRepository(context.sessionFactory)

    private val router = Router.router(context.vertx).apply {
        mountSubRouter("/health", createHealthcheckRouter(context))

        mountSubRouter("/api/v1/client", createClientRouter(context, clientRepository))

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

    fun handle(request: HttpServerRequest) {
        router.handle(request)
    }
}

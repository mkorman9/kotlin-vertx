package com.github.mkorman9.vertx

import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.Router

class MainRouter(
    private val context: AppContext
) {
    private val clientsRepository: ClientRepository = ClientRepository(context.sessionFactory)

    private val router = Router.router(context.vertx).apply {
        get("/").handler { ctx ->
            ctx.response().endWithJson(StatusDTO(
                status = "OK"
            ))
        }

        get("/clients").handler { ctx ->
            clientsRepository.findClients()
                .onSuccess { clientsList -> ctx.response().endWithJson(clientsList) }
                .onFailure { failure -> ctx.fail(500, failure) }
        }
    }

    fun handle(request: HttpServerRequest) {
        router.handle(request)
    }
}

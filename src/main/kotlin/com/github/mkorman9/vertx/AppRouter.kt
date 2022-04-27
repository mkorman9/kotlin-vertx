package com.github.mkorman9.vertx

import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.Router
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory

class AppRouter(
    private val vertx: Vertx,
    private val sessionFactory: SessionFactory
) {
    private val clientsRepository: ClientRepository = ClientRepository(sessionFactory)

    private val router = Router.router(vertx).apply {
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

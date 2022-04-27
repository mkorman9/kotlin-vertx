package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.AppContext
import com.github.mkorman9.vertx.endWithJson
import io.vertx.ext.web.Router

fun createClientRouter(context: AppContext): Router {
    val clientsRepository = ClientRepository(context.sessionFactory)

    return Router.router(context.vertx).apply {
        get("/").handler { ctx ->
            clientsRepository.findClients()
                .onSuccess { clientsList -> ctx.response().endWithJson(clientsList) }
                .onFailure { failure -> ctx.fail(500, failure) }
        }
    }
}

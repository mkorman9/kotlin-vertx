package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.AppContext
import com.github.mkorman9.vertx.StatusDTO
import com.github.mkorman9.vertx.endWithJson
import io.vertx.ext.web.Router

fun createClientRouter(context: AppContext): Router {
    val clientsRepository = ClientRepository(context.sessionFactory)

    return Router.router(context.vertx).apply {
        get("/").handler { ctx ->
            clientsRepository.findAll()
                .onSuccess { clientsList -> ctx.response().endWithJson(clientsList) }
                .onFailure { failure -> ctx.fail(500, failure) }
        }

        get("/:id").handler { ctx ->
            val id = ctx.pathParam("id")
            clientsRepository.findById(id)
                .onSuccess { client ->
                    if (client != null) {
                        ctx.response().endWithJson(client)
                    } else {
                        ctx.response().setStatusCode(404).endWithJson(StatusDTO(
                            status = "error",
                            message = "client not found"
                        ))
                    }
                }
                .onFailure { failure -> ctx.fail(500, failure) }
        }
    }
}

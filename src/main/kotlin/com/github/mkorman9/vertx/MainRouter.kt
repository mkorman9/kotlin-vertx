package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.createClientRouter
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.Router

class MainRouter(
    private val context: AppContext
) {
    private val router = Router.router(context.vertx).apply {
        get("/").handler { ctx ->
            ctx.response().endWithJson(StatusDTO(
                status = "OK"
            ))
        }

        mountSubRouter("/clients", createClientRouter(context))
    }

    fun handle(request: HttpServerRequest) {
        router.handle(request)
    }
}

package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientEventsWebsocketHandler
import io.vertx.core.http.ServerWebSocket

class WebsocketHandler(context: AppContext) {
    private val clientEventsWebsocketHandler = ClientEventsWebsocketHandler(context)

    fun handle(ws: ServerWebSocket) {
        if (ws.path() == "/api/v1/client/events") {
            clientEventsWebsocketHandler.handle(ws)
        } else {
            ws.reject(404)
        }
    }
}

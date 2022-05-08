package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientEventsWebsocketHandler
import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.core.http.ServerWebSocket

@Singleton
class WebsocketHandler @Inject constructor(
    private val clientEventsWebsocketHandler: ClientEventsWebsocketHandler
) {
    fun handle(ws: ServerWebSocket) {
        if (ws.path() == "/api/v1/client/events") {
            clientEventsWebsocketHandler.handle(ws)
        } else {
            ws.reject(404)
        }
    }
}

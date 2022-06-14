package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientEventsWebsocketApi
import com.google.inject.Injector
import io.vertx.core.Vertx
import io.vertx.core.http.ServerWebSocket

class WebsocketApi (vertx: Vertx, injector: Injector) {
    private val clientEventsWebsocketApi = ClientEventsWebsocketApi(vertx, injector)

    fun handle(ws: ServerWebSocket) {
        if (ws.path() == "/api/v1/client/events") {
            clientEventsWebsocketApi.handle(ws)
        } else {
            ws.reject(404)
        }
    }
}

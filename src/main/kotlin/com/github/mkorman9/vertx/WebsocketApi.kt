package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientEventsWebsocketApi
import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.core.http.ServerWebSocket

@Singleton
class WebsocketApi @Inject constructor(
    private val clientEventsWebsocketApi: ClientEventsWebsocketApi
) {
    fun handle(ws: ServerWebSocket) {
        if (ws.path() == "/api/v1/client/events") {
            clientEventsWebsocketApi.handle(ws)
        } else {
            ws.reject(404)
        }
    }
}

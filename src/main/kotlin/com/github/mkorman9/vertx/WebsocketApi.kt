package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientEventsWebsocketApi
import com.google.inject.Injector
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.core.http.ServerWebSocket

class WebsocketApi (injector: Injector) {
    private val clientEventsWebsocketApi = injector.getInstance<ClientEventsWebsocketApi>()

    fun handle(ws: ServerWebSocket) {
        if (ws.path() == "/api/v1/client/events") {
            clientEventsWebsocketApi.handle(ws)
        } else {
            ws.reject(404)
        }
    }
}

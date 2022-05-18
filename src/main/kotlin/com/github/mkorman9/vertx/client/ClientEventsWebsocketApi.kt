package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.utils.WebsocketStore
import com.google.inject.Singleton
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.Json

@Singleton
class ClientEventsWebsocketApi {
    private val log = LoggerFactory.getLogger(ClientEventsWebsocketApi::class.java)

    private val websockets = WebsocketStore()

    fun handle(ws: ServerWebSocket) {
        val id = websockets.add(ws)

        log.info("Websocket '$id' connected")

        ws.closeHandler {
            websockets.remove(id)
            log.info("Websocket '$id' disconnected")
        }

        ws.textMessageHandler { msg ->
            log.info("Websocket '$id' sent '${msg}'")
            ws.writeTextMessage(msg)
        }
    }

    fun onEvent(clientEvent: ClientEvent) {
        val eventSerialized = Json.encode(clientEvent)

        websockets.list().forEach { ws ->
            ws.writeTextMessage(eventSerialized)
        }
    }
}

package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.utils.WebsocketStore
import com.google.inject.Singleton
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.impl.logging.LoggerFactory

@Singleton
class ClientEventsWebsocketHandler {
    private val log = LoggerFactory.getLogger(ClientEventsWebsocketHandler::class.java)

    companion object {
        val Websockets = WebsocketStore()
    }

    fun handle(ws: ServerWebSocket) {
        val id = Websockets.add(ws)

        log.info("Websocket '$id' connected")

        ws.endHandler {
            Websockets.remove(id)
            log.info("Websocket '$id' disconnected")
        }

        ws.textMessageHandler { msg ->
            log.info("Websocket '$id' sent '${msg}'")
            ws.writeTextMessage(msg)
        }
    }
}

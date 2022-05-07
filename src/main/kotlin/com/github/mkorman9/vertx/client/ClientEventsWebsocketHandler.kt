package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.AppContext
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.impl.logging.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ClientEventsWebsocketHandler(
    private val context: AppContext
) {
    private val log = LoggerFactory.getLogger(ClientEventsWebsocketHandler::class.java)

    companion object {
        val ClientEventsWebsockets = ConcurrentHashMap<UUID, ServerWebSocket>()
    }

    fun handle(ws: ServerWebSocket) {
        val id = UUID.randomUUID()

        ClientEventsWebsockets[id] = ws
        log.info("Websocket '$id' connected")

        ws.endHandler {
            ClientEventsWebsockets.remove(id)
            log.info("Websocket '$id' disconnected")
        }

        ws.textMessageHandler { msg ->
            log.info("Websocket '$id' sent '${msg}'")
            ws.writeTextMessage(msg)
        }
    }
}

package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.utils.VerticleContext
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicLong

class ClientEventsWebsocketApi (context: VerticleContext) {
    companion object {
        private val log = LoggerFactory.getLogger(ClientEventsWebsocketApi::class.java)

        private const val CLEANER_PERIOD: Long = 15 * 1000  // 15 sec
    }

    private val websockets = WebsocketStore()

    init {
        context.vertx.eventBus().consumer<JsonObject>(ClientEventsVerticle.INCOMING_CHANNEL) { message ->
            websockets.forEach { _, wsContext ->
                wsContext.socket.writeTextMessage(message.body().encode())
            }
        }

        context.vertx.setPeriodic(CLEANER_PERIOD) {
            val deadline = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - 15

            websockets.forEach { id, wsContext ->
                if (wsContext.lastHeartbeatTimestamp.get() < deadline) {
                    wsContext.socket.close()
                    websockets.remove(id)
                }
            }
        }
    }

    fun handle(ws: ServerWebSocket) {
        val id = websockets.add(
            WebsocketContext(
                socket = ws,
                lastHeartbeatTimestamp = AtomicLong(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
            )
        )

        log.info("Websocket '$id' connected")

        ws.closeHandler {
            websockets.remove(id)
            log.info("Websocket '$id' disconnected")
        }

        ws.textMessageHandler { msg ->
            log.info("Websocket '$id' sent '${msg}'")

            websockets.execute(id) { wsContext ->
                wsContext.lastHeartbeatTimestamp.set(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
            }
        }
    }
}

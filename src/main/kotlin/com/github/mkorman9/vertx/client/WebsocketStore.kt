package com.github.mkorman9.vertx.client

import io.vertx.core.http.ServerWebSocket
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class WebsocketContext(
    val socket: ServerWebSocket,
    var lastHeartbeatTimestamp: AtomicLong
)

class WebsocketStore {
    private val map = ConcurrentHashMap<UUID, WebsocketContext>()

    fun add(wsContext: WebsocketContext): UUID {
        val id = UUID.randomUUID()
        map[id] = wsContext
        return id
    }

    fun remove(id: UUID) {
        map.remove(id)
    }

    fun forEach(f: (WebsocketContext) -> Unit) {
        map.forEachValue(Long.MAX_VALUE) { context ->
            f(context)
        }
    }

    fun execute(id: UUID, f: (WebsocketContext) -> Unit) {
        map.compute(id) { _, context ->
            if (context != null) {
                f(context)
            }

            context
        }
    }
}

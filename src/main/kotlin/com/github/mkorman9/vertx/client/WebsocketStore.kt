package com.github.mkorman9.vertx.client

import io.vertx.core.http.ServerWebSocket
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class WebsocketStore {
    private val map = ConcurrentHashMap<UUID, ServerWebSocket>()

    fun add(ws: ServerWebSocket): UUID {
        val id = UUID.randomUUID()
        map[id] = ws
        return id
    }

    fun remove(id: UUID) {
        map.remove(id)
    }

    fun list(): Collection<ServerWebSocket> {
        return map.values
    }
}
package com.github.mkorman9.vertx.client

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

class ClientEventsPublisher {
    fun publish(vertx: Vertx, event: ClientEvent) {
        vertx.eventBus().publish(ClientEventsVerticle.OUTGOING_CHANNEL, JsonObject.mapFrom(event))
    }
}

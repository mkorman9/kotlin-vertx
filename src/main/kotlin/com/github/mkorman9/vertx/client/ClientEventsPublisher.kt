package com.github.mkorman9.vertx.client

import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

@Singleton
class ClientEventsPublisher @Inject constructor(
    private val vertx: Vertx
) {
    fun publish(event: ClientEvent) {
        vertx.eventBus().publish(ClientEventsVerticle.PUBLISH_CHANNEL_ADDRESS, JsonObject.mapFrom(event))
    }
}

package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.GCPPubSubClient
import com.google.cloud.pubsub.v1.Publisher
import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import io.vertx.core.json.Json

@Singleton
class ClientEventsPublisher @Inject constructor(
    gcpPubSubClient: GCPPubSubClient
) {
    private val topicName = "client.events"
    private val publisher: Publisher

    init {
        publisher = gcpPubSubClient.createPublisher(topicName)
    }

    fun publish(event: ClientEvent) {
        val data = Json.encode(event)
        val message = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(data)).build()
        publisher.publish(message)
    }
}

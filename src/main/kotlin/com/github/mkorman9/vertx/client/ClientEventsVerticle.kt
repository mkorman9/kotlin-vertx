package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.tools.gcp.GCPPubSubClient
import com.github.mkorman9.vertx.utils.ContextualVerticle
import com.github.mkorman9.vertx.utils.DeployVerticle
import com.google.cloud.pubsub.v1.AckReplyConsumerWithResponse
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.core.buffer.Buffer
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await

@DeployVerticle
class ClientEventsVerticle : ContextualVerticle() {
    companion object {
        private val log = LoggerFactory.getLogger(ClientEventsVerticle::class.java)

        const val OUTGOING_CHANNEL = "client.events.outgoing"
        const val INCOMING_CHANNEL = "client.events.incoming"

        private const val PUBSUB_TOPIC_NAME = "client.events"
    }

    override suspend fun start() {
        val gcpPubSubClient = injector.getInstance<GCPPubSubClient>()

        try {
            gcpPubSubClient.createSubscriber(PUBSUB_TOPIC_NAME, this::incomingMessageHandler).await()

            val pubSubPublisher = gcpPubSubClient.createPublisher(PUBSUB_TOPIC_NAME).await()
            redirectToPubSub(pubSubPublisher)
        } catch (e: Exception) {
            log.error("Failed to deploy ClientEventsVerticle", e)
            throw e
        }
    }

    private fun incomingMessageHandler(message: PubsubMessage, consumer: AckReplyConsumerWithResponse) {
        val event = Json.decodeValue(Buffer.buffer(message.data.toByteArray()), ClientEvent::class.java)

        log.info("ClientEvent has been received $event")

        vertx.eventBus().publish(INCOMING_CHANNEL, JsonObject.mapFrom(event))

        consumer.ack()
    }

    private fun redirectToPubSub(pubSubPublisher: Publisher) {
        vertx.eventBus().consumer<JsonObject>(OUTGOING_CHANNEL) { message ->
            val data = message.body().encode()
            val pubsubMessage = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(data)).build()

            pubSubPublisher.publish(pubsubMessage)
        }
    }
}

package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.utils.gcp.GCPPubSubClient
import com.github.mkorman9.vertx.utils.DeployVerticle
import com.google.cloud.pubsub.v1.AckReplyConsumerWithResponse
import com.google.cloud.pubsub.v1.Publisher
import com.google.inject.Injector
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.core.buffer.Buffer
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

@DeployVerticle
class ClientEventsVerticle(
    injector: Injector
) : CoroutineVerticle() {
    companion object {
        private val log = LoggerFactory.getLogger(ClientEventsVerticle::class.java)

        const val PUBLISH_CHANNEL_ADDRESS = "client.events.publish"
        const val CONSUME_CHANNEL_ADDRESS = "client.events.consume"
    }

    private val gcpPubSubClient = injector.getInstance<GCPPubSubClient>()

    private val topicName = "client.events"

    override suspend fun start() {
        try {
            gcpPubSubClient.createSubscriber(topicName, this::messageHandler).await()
        } catch(e: Exception) {
            log.error("Error while creating ClientsEvents subscriber", e)
        }

        val publisher = gcpPubSubClient.createPublisher(topicName).await()
        createPublishChannel(publisher)

        log.info("ClientEventsVerticle has been deployed")
    }

    private fun messageHandler(message: PubsubMessage, consumer: AckReplyConsumerWithResponse) {
        val event = Json.decodeValue(Buffer.buffer(message.data.toByteArray()), ClientEvent::class.java)

        log.info("ClientEvent has been received $event")

        vertx.eventBus().publish(CONSUME_CHANNEL_ADDRESS, JsonObject.mapFrom(event))

        consumer.ack()
    }

    private fun createPublishChannel(publisher: Publisher) {
        vertx.eventBus().consumer<JsonObject>(PUBLISH_CHANNEL_ADDRESS) { message ->
            val data = message.body().encode()
            val pubsubMessage = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(data)).build()
            publisher.publish(pubsubMessage)
        }
    }
}

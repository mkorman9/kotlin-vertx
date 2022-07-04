package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.tools.aws.SQSClient
import com.github.mkorman9.vertx.tools.aws.SQSDelivery
import com.github.mkorman9.vertx.utils.ContextualVerticle
import com.github.mkorman9.vertx.utils.DeployVerticle
import dev.misfitlabs.kotlinguice4.getInstance
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

        private const val SNS_TOPIC_NAME = "ClientEvents"
    }

    override suspend fun start() {
        val sqsClient = injector.getInstance<SQSClient>()

        try {
            sqsClient.subscribeToTopic(vertx, SNS_TOPIC_NAME, this::incomingSqsDeliveryHandler).await()
            redirectMessageBusToSqs(sqsClient)
        } catch (e: Exception) {
            log.error("Failed to deploy ClientEventsVerticle", e)
            throw e
        }
    }

    private fun incomingSqsDeliveryHandler(delivery: SQSDelivery) {
        val event = Json.decodeValue(delivery.content, ClientEvent::class.java)

        log.info("ClientEvent has been received $event")

        vertx.eventBus().publish(INCOMING_CHANNEL, JsonObject.mapFrom(event))
    }

    private fun redirectMessageBusToSqs(sqsClient: SQSClient) {
        vertx.eventBus().consumer<JsonObject>(OUTGOING_CHANNEL) { message ->
            val data = message.body().encode()
            sqsClient.publishToTopic(vertx, SNS_TOPIC_NAME, data)
        }
    }
}

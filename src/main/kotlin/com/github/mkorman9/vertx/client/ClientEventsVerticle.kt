package com.github.mkorman9.vertx.client

import com.amazonaws.services.sqs.model.Message
import com.github.mkorman9.vertx.tools.aws.SQSClient
import com.github.mkorman9.vertx.tools.aws.getContent
import com.github.mkorman9.vertx.utils.ContextualVerticle
import com.github.mkorman9.vertx.utils.DeployVerticle
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.core.Future
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

        private const val SQS_TOPIC_NAME = "ClientEvents"
    }

    private lateinit var sqsClient: SQSClient

    override suspend fun start() {
        sqsClient = injector.getInstance()

        try {
            sqsClient.createSubscription(vertx, SQS_TOPIC_NAME, this::incomingMessageHandler).await()
            redirectToSqs()
        } catch (e: Exception) {
            log.error("Failed to deploy ClientEventsVerticle", e)
            throw e
        }
    }

    private fun incomingMessageHandler(message: Message): Future<Void> {
        val event = Json.decodeValue(message.getContent(), ClientEvent::class.java)

        log.info("ClientEvent has been received $event")

        vertx.eventBus().publish(INCOMING_CHANNEL, JsonObject.mapFrom(event))

        return Future.succeededFuture()
    }

    private fun redirectToSqs() {
        vertx.eventBus().consumer<JsonObject>(OUTGOING_CHANNEL) { message ->
            val data = message.body().encode()
            sqsClient.publish(vertx, SQS_TOPIC_NAME, data)
        }
    }
}

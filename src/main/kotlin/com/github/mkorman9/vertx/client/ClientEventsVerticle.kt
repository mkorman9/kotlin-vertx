package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.BootstrapVerticle
import com.github.mkorman9.vertx.GCPPubSubClient
import com.github.mkorman9.vertx.utils.DeployVerticle
import com.google.cloud.pubsub.v1.AckReplyConsumerWithResponse
import com.google.inject.Injector
import com.google.pubsub.v1.PubsubMessage
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.core.buffer.Buffer
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.Json
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

@DeployVerticle
class ClientEventsVerticle(
    passedInjector: Injector? = null
) : CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(ClientEventsVerticle::class.java)

    private val injector = passedInjector ?: BootstrapVerticle.injector
    private val gcpPubSubClient = injector.getInstance<GCPPubSubClient>()
    private val clientEventsWebsocketApi = injector.getInstance<ClientEventsWebsocketApi>()

    private val topicName = "client.events"

    override suspend fun start() {
        try {
            vertx.executeBlocking<Void> { call ->
                gcpPubSubClient.createSubscriber(topicName) { m, c -> messageHandler(m, c) }
                    .startAsync()
                    .awaitRunning()

                call.complete()
            }.await()
        } catch(e: Exception) {
            log.error("Error while creating ClientsEvents subscriber", e)
        }

        log.info("ClientEventsVerticle has been deployed")
    }

    private fun messageHandler(message: PubsubMessage, consumer: AckReplyConsumerWithResponse) {
        val event = Json.decodeValue(Buffer.buffer(message.data.toByteArray()), ClientEvent::class.java)

        log.info("ClientEvent has been received $event")

        clientEventsWebsocketApi.onEvent(event)

        consumer.ack()
    }
}

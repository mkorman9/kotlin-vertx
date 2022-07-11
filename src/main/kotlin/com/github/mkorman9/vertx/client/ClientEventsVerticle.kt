package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.tools.aws.SQSClient
import com.github.mkorman9.vertx.utils.ContextualVerticle
import com.github.mkorman9.vertx.utils.DeployVerticle
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.core.impl.logging.LoggerFactory

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
            sqsClient.createTopicSink(vertx, OUTGOING_CHANNEL, SNS_TOPIC_NAME)
            sqsClient.redirectTopicToEventBus(vertx, SNS_TOPIC_NAME, INCOMING_CHANNEL)
        } catch (e: Exception) {
            log.error("Failed to deploy ClientEventsVerticle", e)
            throw e
        }
    }
}

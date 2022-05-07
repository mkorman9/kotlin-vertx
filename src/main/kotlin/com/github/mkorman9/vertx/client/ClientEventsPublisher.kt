package com.github.mkorman9.vertx.client

import io.vertx.core.json.Json
import io.vertx.rabbitmq.RabbitMQClient

class ClientEventsPublisher(
    private val rabbitMQClient: RabbitMQClient
) {
    private val exchangeName = "client.events"

    init {
        rabbitMQClient.addConnectionEstablishedCallback { promise ->
            rabbitMQClient.exchangeDeclare(
                exchangeName,
                "fanout",
                false,
                true
            )
                .onComplete { promise.complete() }
        }
    }

    fun publish(event: ClientEvent) {
        rabbitMQClient.basicPublish("client.events", "", Json.encodeToBuffer(event))
    }
}

package com.github.mkorman9.vertx.client

import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.core.json.Json
import io.vertx.rabbitmq.RabbitMQClient

@Singleton
class ClientEventsPublisher @Inject constructor(
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
        rabbitMQClient.basicPublish(exchangeName, "", Json.encodeToBuffer(event))
    }
}

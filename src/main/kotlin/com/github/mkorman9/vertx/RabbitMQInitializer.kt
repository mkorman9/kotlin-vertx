package com.github.mkorman9.vertx

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.rabbitmq.RabbitMQClient
import io.vertx.rabbitmq.RabbitMQOptions

class RabbitMQInitializer {
    private lateinit var rabbitMQClient: RabbitMQClient

    fun start(vertx: Vertx, config: JsonObject): Future<RabbitMQClient> {
        val uri = config.getJsonObject("rabbitmq")?.getString("uri")
            ?: throw RuntimeException("rabbitmq.uri is missing from config")

        rabbitMQClient = RabbitMQClient.create(vertx, RabbitMQOptions()
            .setUri(uri)
            .setAutomaticRecoveryEnabled(false)
            .setReconnectAttempts(Integer.MAX_VALUE)
            .setReconnectInterval(1000)
        )

        return rabbitMQClient.start()
            .map { rabbitMQClient }
    }

    fun stop(): Future<Void> {
        return rabbitMQClient.stop()
    }
}

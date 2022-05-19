package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.BootstrapVerticle
import com.github.mkorman9.vertx.utils.DeployVerticle
import com.google.inject.Injector
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.Json
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.rabbitmq.RabbitMQClient
import io.vertx.rabbitmq.RabbitMQMessage
import java.util.*

@DeployVerticle
class ClientEventsVerticle(
    passedInjector: Injector? = null
) : CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(ClientEventsVerticle::class.java)

    private val injector = passedInjector ?: BootstrapVerticle.injector
    private val rabbitMQClient = injector.getInstance<RabbitMQClient>()
    private val clientEventsWebsocketApi = injector.getInstance<ClientEventsWebsocketApi>()

    private val exchangeName = "client.events"

    override suspend fun start() {
        rabbitMQClient.addConnectionEstablishedCallback { promise ->
            rabbitMQClient.exchangeDeclare(
                exchangeName,
                "fanout",
                false,
                true
            )
                .compose {
                    rabbitMQClient.queueDeclare(
                        "",
                        false,
                        true,
                        true
                    )
                }
                .compose { declare ->
                    rabbitMQClient.queueBind(declare.queue, exchangeName, "")
                        .map { declare.queue }
                }
                .compose { queue ->
                    rabbitMQClient.basicConsumer(queue)
                }
                .onSuccess { consumer -> consumer.handler { messageHandler(it) } }
                .onFailure { failure -> log.error("Failed to define a consumer for ClientEvents", failure) }
                .onComplete { promise.complete() }
        }

        log.info("ClientEventsVerticle has been deployed")
    }

    private fun messageHandler(message: RabbitMQMessage) {
        val event = Json.decodeValue(message.body(), ClientEvent::class.java)

        log.info("ClientEvent has been received $event")

        clientEventsWebsocketApi.onEvent(event)
    }
}

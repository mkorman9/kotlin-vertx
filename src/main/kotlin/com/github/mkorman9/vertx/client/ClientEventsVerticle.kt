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

    private val exchangeName = "client.events"
    private val queueName = "client.events.${UUID.randomUUID()}"

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
                        queueName,
                        false,
                        true,
                        true
                    )
                }
                .compose { declare ->
                    rabbitMQClient.queueBind(declare.queue, exchangeName, "")
                }
                .onComplete { promise.complete() }
        }

        try {
            val consumer = rabbitMQClient.basicConsumer(queueName).await()
            consumer.handler { messageHandler(it) }
        } catch (t: Throwable) {
            log.error("Failed to define a consumer for ClientEvents", t)
            throw t
        }

        log.info("ClientEventsVerticle has been deployed")
    }

    private fun messageHandler(message: RabbitMQMessage) {
        val event = Json.decodeValue(message.body(), ClientEvent::class.java)

        log.info("ClientEvent has been received $event")

        ClientEventsWebsocketApi.Websockets.list().forEach { ws ->
            ws.writeTextMessage(Json.encode(event))
        }
    }
}

package com.github.mkorman9.vertx.tools.aws

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.Message
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Vertx
import java.util.concurrent.atomic.AtomicBoolean

internal class SQSSubscription(
    val subscriptionArn: String,
    private val queueUrl: String,
    private val handler: (Message) -> Future<Void>,
    private val vertx: Vertx,
    private val sqsClient: AmazonSQS
) {
    companion object {
        private const val RECEIVE_DELAY: Long = 100
    }

    private val handlerFinished = AtomicBoolean(true)

    private val timerId: Long = vertx.setPeriodic(RECEIVE_DELAY) {
        if (handlerFinished.get()) {
            handlerFinished.set(false)

            val messages = sqsClient.receiveMessage(queueUrl).messages
            if (messages.isEmpty()) {
                handlerFinished.set(true)
            } else {
                val futures = mutableListOf<Future<*>>()

                messages.forEach { message ->
                    val f = handler(message)
                    futures.add(f)
                }

                CompositeFuture.all(futures)
                    .onComplete { handlerFinished.set(true) }
            }
        }
    }

    fun stop() {
        vertx.cancelTimer(timerId)
    }
}

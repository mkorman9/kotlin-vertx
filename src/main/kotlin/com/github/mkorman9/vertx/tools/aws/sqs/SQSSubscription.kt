package com.github.mkorman9.vertx.tools.aws.sqs

import com.amazonaws.AbortedException
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

internal class SQSSubscription(
    private val queueUrl: String,
    private val handler: (SQSDelivery) -> Unit,
    private val vertx: Vertx,
    private val sqsClient: AmazonSQS
) {
    companion object {
        private val log = LoggerFactory.getLogger(SQSSubscription::class.java)

        private const val RECEIVE_DELAY: Long = 10  // 10 ms
        private const val LONG_POLLING_TIME_SEC = 20
    }

    private val pollingInProgress = AtomicBoolean(false)
    private val thread = Thread {
        while (true) {
            receiveMessages()

            try {
                Thread.sleep(RECEIVE_DELAY)
            } catch (e: InterruptedException) {
                // ignore
            }
        }
    }

    init {
        thread.start()
    }

    fun stop() {
        thread.interrupt()
    }

    private fun receiveMessages() {
        if (pollingInProgress.get()) {
            return
        }

        pollingInProgress.set(true)

        try {
            val messages = sqsClient.receiveMessage(
                ReceiveMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withWaitTimeSeconds(LONG_POLLING_TIME_SEC)
            ).messages

            vertx.runOnContext {
                messages.forEach { message ->
                    handler(SQSDelivery(message))
                }
            }

            messages.forEach { message ->
                sqsClient.deleteMessage(queueUrl, message.receiptHandle)
            }
        } catch (e: AbortedException) {
            // ignore
        } catch (e: Exception) {
            log.error("Error while receiving SQS messages from queue '${queueUrl}'", e)
        } finally {
            pollingInProgress.set(false)
        }
    }
}

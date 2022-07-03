package com.github.mkorman9.vertx.tools.aws

import com.amazon.sqs.javamessaging.DestinationUtils
import com.amazon.sqs.javamessaging.SQSConnectionFactory
import com.amazon.sqs.javamessaging.SQSSession
import io.vertx.core.Vertx
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.Session

internal class SQSSubscription(
    val subscriptionArn: String,
    private val handler: (SQSDelivery) -> Unit,
    private val vertx: Vertx,
    queueName: String,
    queueUrl: String,
    sqsConnectionFactory: SQSConnectionFactory,
    autoAck: Boolean
): MessageListener {
    private val connection = sqsConnectionFactory.createConnection()

    init {
        val ackMode = if (autoAck) {
            Session.AUTO_ACKNOWLEDGE
        } else {
            SQSSession.UNORDERED_ACKNOWLEDGE
        }

        val session = connection.createSession(false, ackMode)
        val consumer = session.createConsumer(DestinationUtils.createDestination(queueName, queueUrl))

        consumer.messageListener = this

        connection.start()
    }

    override fun onMessage(message: Message?) {
        if (message != null) {
            vertx.runOnContext {
                handler(SQSDelivery(message))
            }
        }
    }

    fun stop() {
        connection.stop()
    }
}

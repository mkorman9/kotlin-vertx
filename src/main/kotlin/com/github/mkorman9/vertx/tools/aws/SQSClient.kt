package com.github.mkorman9.vertx.tools.aws

import com.amazonaws.ClientConfiguration
import com.amazonaws.Protocol
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.amazonaws.services.sns.model.PublishRequest
import com.amazonaws.services.sns.util.Topics
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.amazonaws.services.sqs.model.SendMessageRequest
import com.github.mkorman9.vertx.utils.Config
import com.github.mkorman9.vertx.utils.get
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.impl.ConcurrentHashSet
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class SQSClient private constructor(
    config: Config
) {
    companion object {
        private val log = LoggerFactory.getLogger(SQSClient::class.java)

        fun create(config: Config): SQSClient {
            return SQSClient(config)
        }
    }

    private val sqsClient: AmazonSQS
    private val snsClient: AmazonSNS
    private val emulatorEnabled: Boolean
    private val emulatorAddress: String

    private val topicCache: ConcurrentHashMap<String, String> = ConcurrentHashMap()
    private val queueCache: ConcurrentHashMap<String, String> = ConcurrentHashMap()
    private val ephemeralQueues: ConcurrentHashSet<String> = ConcurrentHashSet()
    private val activeSubscriptions: MutableList<SQSSubscription> = mutableListOf()
    private val ephemeralSubscriptions: ConcurrentHashSet<String> = ConcurrentHashSet()

    init {
        emulatorEnabled = config.get<Boolean>("aws.sqs.emulator.enabled") ?: false
        emulatorAddress = config.get<String>("aws.sqs.emulator.address") ?: "localhost:4100"

        val sqsBuilder = AmazonSQSClientBuilder.standard()
        val snsBuilder = AmazonSNSClientBuilder.standard()

        if (emulatorEnabled) {
            sqsBuilder
                .withEndpointConfiguration(
                    AwsClientBuilder.EndpointConfiguration(
                        emulatorAddress,
                        Regions.US_EAST_1.name
                    )
                )
                .withCredentials(
                    AWSStaticCredentialsProvider(
                        BasicAWSCredentials("ACCESS_KEY", "SECRET_ACCESS_KEY")
                    )
                )
                .withClientConfiguration(
                    ClientConfiguration()
                        .withProtocol(Protocol.HTTP)
                )

            snsBuilder
                .withEndpointConfiguration(
                    AwsClientBuilder.EndpointConfiguration(
                        emulatorAddress,
                        Regions.US_EAST_1.name
                    )
                )
                .withCredentials(
                    AWSStaticCredentialsProvider(
                        BasicAWSCredentials("ACCESS_KEY", "SECRET_ACCESS_KEY")
                    )
                )
                .withClientConfiguration(
                    ClientConfiguration()
                        .withProtocol(Protocol.HTTP)
                )
        }

        sqsClient = sqsBuilder.build()
        snsClient = snsBuilder.build()
    }

    fun close() {
        activeSubscriptions.forEach { subscription ->
            try {
                subscription.stop()
            } catch (e: Exception) {
                log.error("Failed to unsubscribe SNS topic", e)
            }
        }

        ephemeralSubscriptions.forEach { subscriptionArn ->
            try {
                snsClient.unsubscribe(subscriptionArn)
            } catch (e: Exception) {
                log.error("Failed to remove SNS subscription", e)
            }
        }

        ephemeralQueues.forEach { queueUrl ->
            try {
                sqsClient.deleteQueue(queueUrl)
            } catch (e: Exception) {
                log.error("Failed to delete ephemeral SQS queue", e)
            }
        }
    }

    fun publishToTopic(vertx: Vertx, topicName: String, message: String): Future<Void> {
        return vertx.executeBlocking { call ->
            try {
                val topicArn = getTopic(topicName)

                snsClient.publish(
                    PublishRequest()
                        .withTopicArn(topicArn)
                        .withMessage(message)
                )

                call.complete()
            } catch (e: Exception) {
                call.fail(e)
            }
        }
    }

    fun publishToQueue(vertx: Vertx, queueName: String, message: String): Future<Void> {
        return vertx.executeBlocking { call ->
            try {
                val queueUrl = getQueue(queueName)

                sqsClient.sendMessage(
                    SendMessageRequest()
                        .withQueueUrl(queueUrl)
                        .withMessageBody(message)
                )

                call.complete()
            } catch (e: Exception) {
                call.fail(e)
            }
        }
    }

    fun subscribeToTopic(vertx: Vertx, topicName: String, handler: (SQSDelivery) -> Unit): Future<Void> {
        return vertx.executeBlocking { call ->
            try {
                val topicArn = getTopic(topicName)
                val queueUrl = getQueue("${topicName}_${UUID.randomUUID()}")
                ephemeralQueues.add(queueUrl)

                val subscriptionArn = Topics.subscribeQueue(snsClient, sqsClient, topicArn, queueUrl)
                ephemeralSubscriptions.add(subscriptionArn)

                activeSubscriptions.add(
                    SQSSubscription(
                        queueUrl = queueUrl,
                        handler = handler,
                        vertx = vertx,
                        sqsClient = sqsClient
                    )
                )

                call.complete()
            } catch (e: Exception) {
                call.fail(e)
            }
        }
    }

    fun subscribeToQueue(vertx: Vertx, queueName: String, handler: (SQSDelivery) -> Unit): Future<Void> {
        return vertx.executeBlocking { call ->
            try {
                val queueUrl = getQueue(queueName)

                activeSubscriptions.add(
                    SQSSubscription(
                        queueUrl = queueUrl,
                        handler = handler,
                        vertx = vertx,
                        sqsClient = sqsClient
                    )
                )

                call.complete()
            } catch (e: Exception) {
                call.fail(e)
            }
        }
    }

    fun createTopicSink(vertx: Vertx, messageBusAddress: String, topicName: String) {
        vertx.eventBus().consumer<JsonObject>(messageBusAddress) { message ->
            publishToTopic(vertx, topicName, message.body().encode())
        }
    }

    fun createQueueSink(vertx: Vertx, messageBusAddress: String, queueName: String) {
        vertx.eventBus().consumer<JsonObject>(messageBusAddress) { message ->
            publishToQueue(vertx, queueName, message.body().encode())
        }
    }

    fun redirectTopicToEventBus(vertx: Vertx, topicName: String, eventBusAddress: String) {
        subscribeToTopic(vertx, topicName) { message ->
            vertx.eventBus().publish(eventBusAddress, JsonObject(message.content))
        }
    }

    fun redirectQueueToEventBus(vertx: Vertx, queueName: String, eventBusAddress: String) {
        subscribeToQueue(vertx, queueName) { message ->
            vertx.eventBus().publish(eventBusAddress, JsonObject(message.content))
        }
    }

    private fun getTopic(topicName: String): String {
        val topicArn = topicCache[topicName]
        if (topicArn != null) {
            return topicArn
        }

        val newTopicArn = snsClient.createTopic(topicName).topicArn

        topicCache[topicName] = newTopicArn
        return newTopicArn
    }

    private fun getQueue(queueName: String): String {
        val queueUrl = queueCache[queueName]
        if (queueUrl != null) {
            return queueUrl
        }

        val newQueueUrl = fixEmulatorQueueUrl(
            sqsClient.createQueue(queueName).queueUrl
        )

        queueCache[queueName] = newQueueUrl
        return newQueueUrl
    }

    private fun fixEmulatorQueueUrl(queueUrl: String): String {
        return if (emulatorEnabled) {
            queueUrl.replace("us-east-1.goaws.com:4100", emulatorAddress)
        } else {
            queueUrl
        }
    }
}

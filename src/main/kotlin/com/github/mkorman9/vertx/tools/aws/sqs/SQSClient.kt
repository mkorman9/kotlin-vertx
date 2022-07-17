package com.github.mkorman9.vertx.tools.aws.sqs

import com.amazonaws.ClientConfiguration
import com.amazonaws.Protocol
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.sns.AmazonSNSAsync
import com.amazonaws.services.sns.AmazonSNSAsyncClientBuilder
import com.amazonaws.services.sns.model.CreateTopicResult
import com.amazonaws.services.sns.model.PublishRequest
import com.amazonaws.services.sns.model.PublishResult
import com.amazonaws.services.sns.util.Topics
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import com.amazonaws.services.sqs.model.CreateQueueResult
import com.amazonaws.services.sqs.model.SendMessageRequest
import com.amazonaws.services.sqs.model.SendMessageResult
import com.github.mkorman9.vertx.tools.aws.createAsyncHandler
import com.github.mkorman9.vertx.utils.Config
import com.github.mkorman9.vertx.utils.get
import io.vertx.core.Future
import io.vertx.core.Promise
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

    private val sqsClient: AmazonSQSAsync
    private val snsClient: AmazonSNSAsync
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

        val sqsBuilder = AmazonSQSAsyncClientBuilder.standard()
        val snsBuilder = AmazonSNSAsyncClientBuilder.standard()

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

    fun publishToTopic(topicName: String, message: String): Future<PublishResult> {
        val promise = Promise.promise<PublishResult>()

        getTopic(topicName)
            .onSuccess { topicArn ->
                snsClient.publishAsync(
                    PublishRequest()
                        .withTopicArn(topicArn)
                        .withMessage(message),
                    createAsyncHandler(promise)
                )
            }
            .onFailure { cause ->
                promise.fail(cause)
            }

        return promise.future()
    }

    fun publishToQueue(queueName: String, message: String): Future<SendMessageResult> {
        val promise = Promise.promise<SendMessageResult>()

        getQueue(queueName)
            .onSuccess { queueUrl ->
                sqsClient.sendMessageAsync(
                    SendMessageRequest()
                        .withQueueUrl(queueUrl)
                        .withMessageBody(message),
                    createAsyncHandler(promise)
                )
            }
            .onFailure { cause ->
                promise.fail(cause)
            }

        return promise.future()
    }

    fun subscribeToTopic(vertx: Vertx, topicName: String, handler: (SQSDelivery) -> Unit): Future<Void> {
        val promise = Promise.promise<Void>()

        getTopic(topicName)
            .compose { topicArn ->
                getQueue("${topicName}_${UUID.randomUUID()}")
                    .map { queueUrl -> Pair(topicArn, queueUrl) }
            }
            .onSuccess { (topicArn, queueUrl) ->
                ephemeralQueues.add(queueUrl)

                vertx.executeBlocking<Void> { call ->
                    val subscriptionArn = Topics.subscribeQueue(snsClient, sqsClient, topicArn, queueUrl)
                    ephemeralSubscriptions.add(subscriptionArn)

                    call.complete()
                }

                activeSubscriptions.add(
                    SQSSubscription(
                        queueUrl = queueUrl,
                        handler = handler,
                        vertx = vertx,
                        sqsClient = sqsClient
                    )
                )

                promise.complete()
            }
            .onFailure { cause ->
                promise.fail(cause)
            }

        return promise.future()
    }

    fun subscribeToQueue(vertx: Vertx, queueName: String, handler: (SQSDelivery) -> Unit): Future<Void> {
        val promise = Promise.promise<Void>()

        getQueue(queueName)
            .onSuccess { queueUrl ->
                activeSubscriptions.add(
                    SQSSubscription(
                        queueUrl = queueUrl,
                        handler = handler,
                        vertx = vertx,
                        sqsClient = sqsClient
                    )
                )

                promise.complete()
            }
            .onFailure { cause ->
                promise.fail(cause)
            }

        return promise.future()
    }

    fun createTopicSink(vertx: Vertx, messageBusAddress: String, topicName: String) {
        vertx.eventBus().consumer<JsonObject>(messageBusAddress) { message ->
            publishToTopic(topicName, message.body().encode())
        }
    }

    fun createQueueSink(vertx: Vertx, messageBusAddress: String, queueName: String) {
        vertx.eventBus().consumer<JsonObject>(messageBusAddress) { message ->
            publishToQueue(queueName, message.body().encode())
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

    private fun getTopic(topicName: String): Future<String> {
        val topicArn = topicCache[topicName]
        if (topicArn != null) {
            return Future.succeededFuture(topicArn)
        }

        val promise = Promise.promise<CreateTopicResult>()

        snsClient.createTopicAsync(
            topicName,
            createAsyncHandler(promise)
        )

        return promise.future()
            .map { result ->
                result.topicArn
            }
            .map { newTopicArn ->
                topicCache[topicName] = newTopicArn
                newTopicArn
            }
    }

    private fun getQueue(queueName: String): Future<String> {
        val queueUrl = queueCache[queueName]
        if (queueUrl != null) {
            return Future.succeededFuture(queueUrl)
        }

        val promise = Promise.promise<CreateQueueResult>()

        sqsClient.createQueueAsync(
            queueName,
            createAsyncHandler(promise)
        )

        return promise.future()
            .map { result ->
                fixEmulatorQueueUrl(result.queueUrl)
            }
            .map { newQueueUrl ->
                queueCache[queueName] = newQueueUrl
                newQueueUrl
            }
    }

    private fun fixEmulatorQueueUrl(queueUrl: String): String {
        return if (emulatorEnabled) {
            queueUrl.replace("us-east-1.goaws.com:4100", emulatorAddress)
        } else {
            queueUrl
        }
    }
}

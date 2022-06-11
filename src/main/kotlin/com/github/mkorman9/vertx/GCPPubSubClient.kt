package com.github.mkorman9.vertx

import com.google.api.gax.core.CredentialsProvider
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider
import com.google.api.gax.rpc.ApiException
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.api.gax.rpc.StatusCode
import com.google.api.gax.rpc.TransportChannelProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.pubsub.v1.*
import com.google.cloud.pubsub.v1.stub.PublisherStubSettings
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings
import com.google.pubsub.v1.*
import io.grpc.ManagedChannelBuilder
import io.vertx.config.ConfigRetriever
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GCPPubSubClient @Inject constructor(
    private val vertx: Vertx,
    configRetriever: ConfigRetriever
) {
    private val projectId: String
    private val credentialsProvider: CredentialsProvider

    private val channelProvider: TransportChannelProvider
    private val topicAdminClient: TopicAdminClient
    private val subscriptionAdminClient: SubscriptionAdminClient

    private val publishers = mutableListOf<Publisher>()
    private val subscribers = mutableListOf<Subscriber>()
    private val ephemeralSubscriptions = mutableListOf<ProjectSubscriptionName>()

    init {
        val config = configRetriever.cachedConfig
        val gcpConfig = config.getJsonObject("gcp")

        projectId = gcpConfig?.getString("projectId") ?: "default-project-id"

        val emulatorConfig = gcpConfig?.getJsonObject("pubsub")?.getJsonObject("emulator")
        val emulatorEnabled = emulatorConfig?.getBoolean("enabled") ?: false
        val emulatorAddress = emulatorConfig?.getString("address") ?: "localhost:8538"

        credentialsProvider = if (emulatorEnabled) {
            NoCredentialsProvider.create()
        } else {
            FixedCredentialsProvider.create(GoogleCredentials.getApplicationDefault())
        }

        channelProvider = createTransportChannelProvider(emulatorEnabled, emulatorAddress)
        topicAdminClient = createTopicClient()
        subscriptionAdminClient = createSubscriptionAdminClient()
    }

    fun stop() {
        publishers.forEach {
            it.shutdown()
        }

        subscribers.forEach {
            it.stopAsync().awaitTerminated()
        }

        ephemeralSubscriptions.forEach {
            subscriptionAdminClient.deleteSubscription(it.toString())
        }
    }

    fun createPublisher(topic: String): Future<Publisher> {
        return vertx.executeBlocking { call ->
            createTopic(topic)

            val topicName = TopicName.of(projectId, topic)

            val publisher = Publisher.newBuilder(topicName)
                .setChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build()

            publishers.add(publisher)

            call.complete(publisher)
        }
    }

    fun createSubscriber(
        subscription: String,
        topic: String,
        receiver: MessageReceiverWithAckResponse,
        subscriptionCustomizer: Handler<Subscription.Builder>? = null
    ): Future<Subscriber> {
        return vertx.executeBlocking { call ->
            createSubscription(subscription, topic, subscriptionCustomizer)

            val subscriptionName = ProjectSubscriptionName.newBuilder()
                .setProject(projectId)
                .setSubscription(subscription)
                .build()

            val subscriber = Subscriber.newBuilder(subscriptionName, receiver)
                .setChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build()

            subscribers.add(subscriber)

            subscriber
                .startAsync()
                .awaitRunning()

            call.complete(subscriber)
        }
    }

    fun createSubscriber(
        topic: String,
        receiver: MessageReceiverWithAckResponse,
        subscriptionCustomizer: Handler<Subscription.Builder>? = null
    ): Future<Subscriber> {
        return vertx.executeBlocking { call ->
            val subscription = "$topic.${UUID.randomUUID()}"

            createSubscription(subscription, topic, subscriptionCustomizer)

            val subscriptionName = ProjectSubscriptionName.newBuilder()
                .setProject(projectId)
                .setSubscription(subscription)
                .build()

            ephemeralSubscriptions.add(subscriptionName)

            val subscriber = Subscriber.newBuilder(subscriptionName, receiver)
                .setChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build()

            subscribers.add(subscriber)

            subscriber
                .startAsync()
                .awaitRunning()

            call.complete(subscriber)
        }
    }

    private fun createSubscription(
        name: String,
        topic: String,
        subscriptionCustomizer: Handler<Subscription.Builder>?
    ): Subscription {
        createTopic(topic)

        return try {
            val subscriptionBuilder = Subscription.newBuilder()
                .setName(SubscriptionName.of(projectId, name).toString())
                .setTopic(TopicName.of(projectId, topic).toString())
                .setPushConfig(PushConfig.getDefaultInstance())
                .setAckDeadlineSeconds(0)

            subscriptionCustomizer?.handle(subscriptionBuilder)

            subscriptionAdminClient.createSubscription(subscriptionBuilder.build())
        } catch (e: ApiException) {
            if (e.statusCode.code == StatusCode.Code.ALREADY_EXISTS) {
                subscriptionAdminClient.getSubscription(SubscriptionName.of(projectId, name).toString())
            } else {
                throw e
            }
        }
    }

    private fun createTopic(topic: String): Topic {
        val topicName = TopicName.of(projectId, topic)

        return try {
            topicAdminClient.createTopic(topicName)
        } catch (e: ApiException) {
            if (e.statusCode.code == StatusCode.Code.ALREADY_EXISTS) {
                topicAdminClient.getTopic(topicName)
            } else {
                throw e
            }
        }
    }

    private fun createTransportChannelProvider(
        emulatorEnabled: Boolean,
        emulatorAddress: String
    ): TransportChannelProvider {
        if (emulatorEnabled) {
            val managedChannel = ManagedChannelBuilder
                .forTarget(emulatorAddress)
                .usePlaintext()
                .build()

            return FixedTransportChannelProvider.create(GrpcTransportChannel.create(managedChannel))
        }

        return InstantiatingGrpcChannelProvider.newBuilder().build()
    }

    private fun createTopicClient(): TopicAdminClient {
        val settings = PublisherStubSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentialsProvider)
            .build()

        return TopicAdminClient.create(TopicAdminSettings.create(settings))
    }

    private fun createSubscriptionAdminClient(): SubscriptionAdminClient {
        val settings = SubscriberStubSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentialsProvider)
            .build()

        return SubscriptionAdminClient.create(SubscriptionAdminSettings.create(settings))
    }
}

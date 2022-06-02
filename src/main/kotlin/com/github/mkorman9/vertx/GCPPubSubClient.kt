package com.github.mkorman9.vertx

import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider
import com.google.api.gax.rpc.ApiException
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.api.gax.rpc.StatusCode
import com.google.api.gax.rpc.TransportChannelProvider
import com.google.cloud.pubsub.v1.MessageReceiverWithAckResponse
import com.google.cloud.pubsub.v1.Publisher
import com.google.cloud.pubsub.v1.Subscriber
import com.google.cloud.pubsub.v1.SubscriptionAdminClient
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings
import com.google.cloud.pubsub.v1.TopicAdminClient
import com.google.cloud.pubsub.v1.TopicAdminSettings
import com.google.cloud.pubsub.v1.stub.PublisherStubSettings
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PushConfig
import com.google.pubsub.v1.Subscription
import com.google.pubsub.v1.SubscriptionName
import com.google.pubsub.v1.Topic
import com.google.pubsub.v1.TopicName
import io.grpc.ManagedChannelBuilder
import io.vertx.config.ConfigRetriever
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GCPPubSubClient @Inject constructor(
    private val gcpSettings: GCPSettings,
    configRetriever: ConfigRetriever
) {
    private val channelProvider: TransportChannelProvider
    private val topicAdminClient: TopicAdminClient
    private val subscriptionAdminClient: SubscriptionAdminClient

    private val publishers = mutableListOf<Publisher>()
    private val subscribers = mutableListOf<Subscriber>()
    private val ephemeralSubscriptions = mutableListOf<ProjectSubscriptionName>()

    init {
        val config = configRetriever.cachedConfig
        val pubSubConfig = config.getJsonObject("gcp")?.getJsonObject("pubsub")
        val emulatorConfig = pubSubConfig?.getJsonObject("emulator")

        channelProvider = createTransportChannelProvider(emulatorConfig)
        topicAdminClient = createTopicClient()
        subscriptionAdminClient = createSubscriptionAdminClient()
    }

    fun stop(vertx: Vertx) {
        vertx.executeBlocking<Void> { call ->
            publishers.forEach {
                it.shutdown()
            }

            subscribers.forEach {
                it.stopAsync().awaitTerminated()
            }

            ephemeralSubscriptions.forEach {
                subscriptionAdminClient.deleteSubscription(it.toString())
            }

            call.complete()
        }
    }

    fun createPublisher(topic: String): Publisher {
        createTopic(topic)

        val topicName = TopicName.of(gcpSettings.projectId, topic)

        val publisher = Publisher.newBuilder(topicName)
            .setChannelProvider(channelProvider)
            .setCredentialsProvider(gcpSettings.credentialsProvider)
            .build()

        publishers.add(publisher)

        return publisher
    }

    fun createSubscriber(subscription: String, topic: String, receiver: MessageReceiverWithAckResponse): Subscriber {
        createSubscription(subscription, topic)

        val subscriptionName = ProjectSubscriptionName.newBuilder()
            .setProject(gcpSettings.projectId)
            .setSubscription(subscription)
            .build()

        val subscriber = Subscriber.newBuilder(subscriptionName, receiver)
            .setChannelProvider(channelProvider)
            .setCredentialsProvider(gcpSettings.credentialsProvider)
            .build()

        subscribers.add(subscriber)

        return subscriber
    }

    fun createSubscriber(topic: String, receiver: MessageReceiverWithAckResponse): Subscriber {
        val subscription = "$topic.${UUID.randomUUID()}"

        createSubscription(subscription, topic)

        val subscriptionName = ProjectSubscriptionName.newBuilder()
            .setProject(gcpSettings.projectId)
            .setSubscription(subscription)
            .build()

        ephemeralSubscriptions.add(subscriptionName)

        val subscriber = Subscriber.newBuilder(subscriptionName, receiver)
            .setChannelProvider(channelProvider)
            .setCredentialsProvider(gcpSettings.credentialsProvider)
            .build()

        subscribers.add(subscriber)

        return subscriber
    }

    private fun createSubscription(name: String, topic: String): Subscription {
        createTopic(topic)

        return try {
            subscriptionAdminClient.createSubscription(
                Subscription.newBuilder()
                    .setName(SubscriptionName.of(gcpSettings.projectId, name).toString())
                    .setTopic(TopicName.of(gcpSettings.projectId, topic).toString())
                    .setPushConfig(PushConfig.getDefaultInstance())
                    .setAckDeadlineSeconds(0)
                    .build()
            )
        } catch (e: ApiException) {
            if (e.statusCode.code == StatusCode.Code.ALREADY_EXISTS) {
                subscriptionAdminClient.getSubscription(SubscriptionName.of(gcpSettings.projectId, name).toString())
            } else {
                throw e
            }
        }
    }

    private fun createTopic(topic: String): Topic {
        val topicName = TopicName.of(gcpSettings.projectId, topic)

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

    private fun createTransportChannelProvider(emulatorConfig: JsonObject?): TransportChannelProvider {
        val emulatorEnabled = emulatorConfig?.getBoolean("enabled") ?: false
        val emulatorAddress = emulatorConfig?.getString("address") ?: "localhost:8538"

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
            .setCredentialsProvider(gcpSettings.credentialsProvider)
            .build()

        return TopicAdminClient.create(TopicAdminSettings.create(settings))
    }

    private fun createSubscriptionAdminClient(): SubscriptionAdminClient {
        val settings = SubscriberStubSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(gcpSettings.credentialsProvider)
            .build()

        return SubscriptionAdminClient.create(SubscriptionAdminSettings.create(settings))
    }
}

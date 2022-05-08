package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.utils.DeployVerticle
import com.google.inject.Guice
import com.google.inject.Injector
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.impl.launcher.commands.RunCommand
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.rabbitmq.RabbitMQClient
import io.vertx.rabbitmq.RabbitMQOptions
import org.hibernate.reactive.mutiny.Mutiny
import org.reflections.Reflections
import java.io.IOException
import java.time.LocalDateTime
import java.util.jar.Manifest
import javax.persistence.Persistence

class BootstrapVerticle : CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(BootstrapVerticle::class.java)

    companion object {
        lateinit var injector: Injector
    }

    override suspend fun start() {
        try {
            val context = DeploymentContext(
                version = readVersionFromManifest().await(),
                startupTime = LocalDateTime.now(),
                environment = System.getenv("ENVIRONMENT_NAME") ?: "default"
            )

            val configRetriever = createConfigRetriever()
            val config = configRetriever.config.await()

            val sessionFactory = startHibernate(config).await()
            val rabbitMQClient = connectToRabbitMQ(config).await()

            val module = AppModule(vertx, context, configRetriever, sessionFactory, rabbitMQClient)
            injector = Guice.createInjector(module)

            log.info("BootstrapVerticle has been deployed")

            deployVerticles(config)
        } catch (e: Exception) {
            log.error("Failed to deploy BootstrapVerticle", e)
            throw e
        }
    }

    override suspend fun stop() {
        injector.getInstance<RabbitMQClient>().stop().await()
    }

    private fun deployVerticles(config: JsonObject) {
        val packageReflections = Reflections(AppModule.packageName)
        packageReflections.getTypesAnnotatedWith(DeployVerticle::class.java)
            .forEach { c ->
                val verticleName = c.annotations.filterIsInstance<DeployVerticle>()
                    .first()
                    .name
                    .ifEmpty { c.name }
                val verticleConfig = config.getJsonObject(verticleName)

                vertx.deployVerticle(c.name, DeploymentOptions()
                    .setInstances(verticleConfig?.getInteger("instances") ?: 1)
                )
            }
    }

    private fun createConfigRetriever(): ConfigRetriever {
        val store = ConfigStoreOptions()
            .setType("file")
            .setFormat("yaml")
            .setOptional(true)
            .setConfig(JsonObject()
                .put("path", System.getenv().getOrDefault("CONFIG_FILE", "./config.yml"))
            )
        val secretsStore = ConfigStoreOptions()
            .setType("file")
            .setFormat("yaml")
            .setOptional(true)
            .setConfig(JsonObject()
                .put("path", System.getenv().getOrDefault("SECRETS_FILE", "./secrets.yml"))
            )

        return ConfigRetriever.create(vertx, ConfigRetrieverOptions()
            .addStore(store)
            .addStore(secretsStore)
        )
    }

    private fun startHibernate(config: JsonObject): Future<Mutiny.SessionFactory> {
        val uri = config.getJsonObject("db")?.getString("uri")
            ?: throw RuntimeException("db.uri is missing from config")
        val user = config.getJsonObject("db")?.getString("user")
            ?: throw RuntimeException("db.user is missing from config")
        val password = config.getJsonObject("db")?.getString("password")
            ?: throw RuntimeException("db.password is missing from config")

        val props = mapOf(
            "javax.persistence.jdbc.url" to uri,
            "javax.persistence.jdbc.user" to user,
            "javax.persistence.jdbc.password" to password
        )

        return vertx
            .executeBlocking { call ->
                val sessionFactory = Persistence
                    .createEntityManagerFactory("default", props)
                    .unwrap(Mutiny.SessionFactory::class.java)

                call.complete(sessionFactory)
            }
    }

    private fun connectToRabbitMQ(config: JsonObject): Future<RabbitMQClient> {
        val uri = config.getJsonObject("rabbitmq")?.getString("uri")
            ?: throw RuntimeException("rabbitmq.uri is missing from config")

        val client = RabbitMQClient.create(vertx, RabbitMQOptions()
            .setUri(uri)
            .setAutomaticRecoveryEnabled(false)
            .setReconnectAttempts(Integer.MAX_VALUE)
            .setReconnectInterval(1000)
        )

        return client.start().map { client }
    }

    private fun readVersionFromManifest(): Future<String> {
        return vertx.executeBlocking { call ->
            try {
                val resources = RunCommand::class.java.classLoader.getResources("META-INF/MANIFEST.MF")
                while (resources.hasMoreElements()) {
                    resources.nextElement().openStream().use { stream ->
                        val manifest = Manifest(stream)
                        val attributes = manifest.mainAttributes
                        val version = attributes.getValue("Version") ?: "dev"
                        call.complete(version)
                    }
                }
            } catch (e: IOException) {
                call.fail(e)
            }
        }
    }
}

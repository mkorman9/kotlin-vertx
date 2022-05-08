package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.utils.DeployVerticle
import com.google.inject.Guice
import com.google.inject.Injector
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
import org.reflections.Reflections
import java.io.IOException
import java.time.LocalDateTime
import java.util.jar.Manifest

class BootstrapVerticle : CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(BootstrapVerticle::class.java)

    companion object {
        lateinit var injector: Injector
    }

    private val hibernateInitializer = HibernateInitializer()
    private val rabbitMQInitializer = RabbitMQInitializer()

    override suspend fun start() {
        try {
            val context = DeploymentContext(
                version = readVersionFromManifest().await(),
                startupTime = LocalDateTime.now(),
                environment = System.getenv("ENVIRONMENT_NAME") ?: "default"
            )

            val configRetriever = createConfigRetriever()
            val config = configRetriever.config.await()

            val sessionFactory = hibernateInitializer.start(vertx, config).await()
            val rabbitMQClient = rabbitMQInitializer.start(vertx, config).await()

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
        rabbitMQInitializer.stop().await()
        hibernateInitializer.stop(vertx).await()
    }

    private fun deployVerticles(config: JsonObject) {
        val packageReflections = Reflections(AppModule.packageName)
        packageReflections.getTypesAnnotatedWith(DeployVerticle::class.java)
            .forEach { c ->
                val verticleConfig = c.annotations.filterIsInstance<DeployVerticle>()
                    .map { it.configKey }
                    .filter { it.isNotEmpty() }
                    .map { config.getJsonObject(it) }
                    .firstOrNull()

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

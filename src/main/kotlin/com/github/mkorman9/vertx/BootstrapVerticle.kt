package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.utils.DeployVerticle
import com.google.inject.Guice
import com.google.inject.Injector
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
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
            val gcpSettings = GCPSettings.read(vertx, config)
            val firestore = FirestoreInitializer(gcpSettings, config).initialize()

            val module = AppModule(vertx, context, configRetriever, sessionFactory, gcpSettings)
            injector = Guice.createInjector(module)

            deployVerticles(config).await()

            vertx.executeBlocking<Void> { call ->
                // store
                val doc = firestore.collection("users").document("michal")
                val data = mapOf<String, Any>(
                    "firstName" to "Marcin",
                    "lastName" to "Figlarz",
                    "born" to "1960"
                )
                doc.set(data).get()

                // retrieve
                val docs = firestore.collection("users").whereEqualTo("firstName", "Marcin")
                    .get()
                    .get()
                    .documents

                docs.forEach {
                    println(it.data)
                }

                call.complete()
            }.await()

            log.info("BootstrapVerticle has been deployed")
        } catch (e: Exception) {
            log.error("Failed to deploy BootstrapVerticle", e)
            throw e
        }
    }

    override suspend fun stop() {
        injector.getInstance<GCPPubSubClient>().stop()
        hibernateInitializer.stop(vertx).await()

        log.info("BootstrapVerticle has been stopped")
    }

    private fun deployVerticles(config: JsonObject): Future<CompositeFuture> {
        val futures = mutableListOf<Future<*>>()

        val packageReflections = Reflections(AppModule.packageName)
        packageReflections.getTypesAnnotatedWith(DeployVerticle::class.java)
            .forEach { c ->
                val annotation = c.annotations.filterIsInstance<DeployVerticle>()
                    .first()
                val verticleConfig =
                    if (annotation.configKey.isNotEmpty()) config.getJsonObject(annotation.configKey)
                    else null

                val future = vertx.deployVerticle(c.name, DeploymentOptions()
                    .setInstances(verticleConfig?.getInteger("instances") ?: 1)
                    .setWorker(annotation.worker)
                    .setWorkerPoolName(annotation.workerPoolName.ifEmpty { null })
                    .setWorkerPoolSize(annotation.workerPoolSize)
                )
                futures.add(future)
            }

        return CompositeFuture.all(futures)
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
                val resources = BootstrapVerticle::class.java.classLoader.getResources("META-INF/MANIFEST.MF")
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

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
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import org.reflections.Reflections
import java.io.IOException
import java.time.LocalDateTime
import java.util.jar.Manifest
import kotlin.system.exitProcess

class AppBootstrapper {
    private val log = LoggerFactory.getLogger(AppBootstrapper::class.java)

    private val hibernateInitializer = HibernateInitializer()
    private lateinit var injector: Injector

    fun bootstrap(vertx: Vertx) {
        try {
            val context = DeploymentContext(
                version = readVersionFromManifest(),
                startupTime = LocalDateTime.now(),
                environment = System.getenv("ENVIRONMENT_NAME") ?: "default"
            )

            val configRetriever = createConfigRetriever(vertx)
            val config = configRetriever.config
                .toCompletionStage()
                .toCompletableFuture()
                .join()

            val sessionFactory = hibernateInitializer.start(config)

            val module = AppModule(vertx, context, config, sessionFactory)
            injector = Guice.createInjector(module)

            deployVerticles(vertx, injector, config)
                .toCompletionStage()
                .toCompletableFuture()
                .join()

            log.info("App has been bootstrapped successfully")
        } catch (e: Exception) {
            log.error("Failed to bootstrap the app", e)
            exitProcess(1)
        }
    }

    fun shutdown() {
        injector.getInstance<GCPPubSubClient>().stop()
        hibernateInitializer.stop()

        log.info("App has been stopped")
    }

    private fun deployVerticles(vertx: Vertx, injector: Injector, config: JsonObject): Future<CompositeFuture> {
        val futures = mutableListOf<Future<*>>()

        val httpServerInstances = config.getJsonObject("server")?.getInteger("instances") ?: 1
        for (i in 0 until httpServerInstances) {
            futures.add(
                vertx.deployVerticle(HttpServerVerticle(injector))
            )
        }

        val packageReflections = Reflections(AppModule.packageName)
        packageReflections.getTypesAnnotatedWith(DeployVerticle::class.java)
            .forEach { c ->
                val annotation = c.annotations.filterIsInstance<DeployVerticle>()
                    .first()

                val future = vertx.deployVerticle(
                    c.declaredConstructors[0].newInstance(injector) as Verticle,
                    DeploymentOptions()
                        .setWorker(annotation.worker)
                        .setWorkerPoolName(annotation.workerPoolName.ifEmpty { null })
                        .setWorkerPoolSize(annotation.workerPoolSize)
                )
                futures.add(future)
            }

        return CompositeFuture.all(futures)
    }

    private fun createConfigRetriever(vertx: Vertx): ConfigRetriever {
        val configFileStore = ConfigStoreOptions()
            .setType("file")
            .setFormat("yaml")
            .setOptional(true)
            .setConfig(JsonObject()
                .put("path", System.getenv().getOrDefault("CONFIG_FILE", "./config.yml"))
            )
        val secretsFileStore = ConfigStoreOptions()
            .setType("file")
            .setFormat("yaml")
            .setOptional(true)
            .setConfig(JsonObject()
                .put("path", System.getenv().getOrDefault("SECRETS_FILE", "./secrets.yml"))
            )
        val envVarsStore = ConfigStoreOptions()
            .setType("env")

        return ConfigRetriever.create(vertx, ConfigRetrieverOptions()
            .addStore(configFileStore)
            .addStore(secretsFileStore)
            .addStore(envVarsStore)
            .setScanPeriod(0)
        )
            .setConfigurationProcessor { config ->
                val newConfig = mutableMapOf<String, Any>()

                config.map.forEach { entry ->
                    val key = entry.key.lowercase()
                    val value = entry.value

                    val splits = key.split("_")
                    if (splits.size == 1) {
                        newConfig[key] = value
                    } else {
                        var ptr = newConfig
                        splits.forEachIndexed { index, s ->
                            if (index < splits.size - 1) {
                                if (!ptr.containsKey(s)) {
                                    ptr[s] = mutableMapOf<String, Any>()
                                }

                                if (ptr[s] is Map<*, *>) {
                                    @Suppress("UNCHECKED_CAST")
                                    ptr = ptr[s] as MutableMap<String, Any>
                                }
                            } else {
                                ptr[s] = value
                            }
                        }
                    }
                }

                JsonObject(newConfig)
            }
    }

    private fun readVersionFromManifest(): String {
        try {
            val resources = AppBootstrapper::class.java.classLoader.getResources("META-INF/MANIFEST.MF")
            while (resources.hasMoreElements()) {
                resources.nextElement().openStream().use { stream ->
                    val manifest = Manifest(stream)
                    val attributes = manifest.mainAttributes

                    return attributes.getValue("Version") ?: "dev"
                }
            }
        } catch (e: IOException) {
            log.error("Failed to load app version from MANIFEST.MF", e)
            throw e
        }

        return ""
    }
}

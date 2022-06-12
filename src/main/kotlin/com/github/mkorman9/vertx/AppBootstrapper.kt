package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.utils.*
import com.github.mkorman9.vertx.utils.gcp.GCPPubSubClient
import com.github.mkorman9.vertx.utils.hibernate.HibernateInitializer
import com.google.inject.Guice
import com.google.inject.Injector
import io.vertx.core.*
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import java.time.LocalDateTime
import kotlin.system.exitProcess

class AppBootstrapper {
    companion object {
        private val log = LoggerFactory.getLogger(AppBootstrapper::class.java)
    }

    private val hibernateInitializer = HibernateInitializer()
    private lateinit var gcpPubSubClient: GCPPubSubClient

    fun bootstrap(vertx: Vertx) {
        try {
            JsonCodec.configure()

            val context = DeploymentContext(
                version = VersionReader.read(),
                startupTime = LocalDateTime.now(),
                environment = System.getenv("ENVIRONMENT_NAME") ?: "default"
            )
            val config = ConfigReader.read(vertx)

            val sessionFactory = hibernateInitializer.start(config)
            gcpPubSubClient = GCPPubSubClient(vertx, config)

            val module = AppModule(vertx, context, config, sessionFactory, gcpPubSubClient)
            val injector = Guice.createInjector(module)

            deployHttpServer(config, vertx, injector)

            VerticleDeployer.scanAndDeploy(vertx, AppModule.packageName, injector)

            log.info("App has been bootstrapped successfully")
        } catch (e: Exception) {
            log.error("Failed to bootstrap the app", e)
            exitProcess(1)
        }
    }

    fun shutdown() {
        gcpPubSubClient.stop()
        hibernateInitializer.stop()

        log.info("App has been stopped")
    }

    private fun deployHttpServer(config: JsonObject, vertx: Vertx, injector: Injector) {
        val instances = config.getJsonObject("server")?.getInteger("instances")
            ?: Runtime.getRuntime().availableProcessors()

        val futures = mutableListOf<Future<*>>()

        for (i in 0 until instances) {
            futures.add(
                vertx.deployVerticle(HttpServerVerticle(injector))
            )
        }

        CompositeFuture.all(futures)
            .toCompletionStage()
            .toCompletableFuture()
            .join()

        log.info("Successfully deployed $instances HttpServerVerticle instances")
    }
}

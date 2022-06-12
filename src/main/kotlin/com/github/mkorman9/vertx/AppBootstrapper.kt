package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.utils.ConfigReader
import com.github.mkorman9.vertx.utils.InjectorUtils
import com.github.mkorman9.vertx.utils.JsonCodec
import com.github.mkorman9.vertx.utils.VerticleDeployer
import com.github.mkorman9.vertx.utils.gcp.GCPPubSubClient
import com.github.mkorman9.vertx.utils.hibernate.HibernateInitializer
import com.google.inject.Injector
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import kotlin.system.exitProcess

class AppBootstrapper {
    companion object {
        private val log = LoggerFactory.getLogger(AppBootstrapper::class.java)
    }

    private lateinit var sessionFactory: SessionFactory
    private lateinit var gcpPubSubClient: GCPPubSubClient

    fun bootstrap(vertx: Vertx) {
        try {
            JsonCodec.configure()

            val config = ConfigReader.read(vertx)

            sessionFactory = HibernateInitializer.initialize(config)
            gcpPubSubClient = GCPPubSubClient.create(vertx, config)

            val injector = InjectorUtils.createInjector(
                AppModule.PACKAGE_NAME,
                AppModule(vertx, config, sessionFactory, gcpPubSubClient)
            )

            deployHttpServer(config, vertx, injector)

            VerticleDeployer.scanAndDeploy(vertx, AppModule.PACKAGE_NAME, injector)

            log.info("App has been bootstrapped successfully")
        } catch (e: Exception) {
            log.error("Failed to bootstrap the app", e)
            exitProcess(1)
        }
    }

    fun shutdown() {
        gcpPubSubClient.stop()
        sessionFactory.close()

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

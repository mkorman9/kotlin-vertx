package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.tools.gcp.GCPPubSubClient
import com.github.mkorman9.vertx.tools.hibernate.HibernateInitializer
import com.github.mkorman9.vertx.tools.hibernate.LiquibaseExecutor
import com.github.mkorman9.vertx.utils.BootstrapUtils
import com.github.mkorman9.vertx.utils.ConfigReader
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import kotlin.system.exitProcess

class Application {
    companion object {
        private val log = LoggerFactory.getLogger(Application::class.java)

        const val PACKAGE_NAME = "com.github.mkorman9.vertx"
    }

    private lateinit var sessionFactory: SessionFactory
    private lateinit var gcpPubSubClient: GCPPubSubClient

    fun bootstrap(vertx: Vertx) {
        try {
            val config = ConfigReader.read(vertx)

            LiquibaseExecutor.migrateSchema(vertx, config)

            sessionFactory = HibernateInitializer.initialize(vertx, config)
                .toCompletionStage()
                .toCompletableFuture()
                .join()

            gcpPubSubClient = GCPPubSubClient.create(vertx, config)

            BootstrapUtils.bootstrap(
                packageName = PACKAGE_NAME,
                vertx = vertx,
                config = config,
                module = object : KotlinModule() {
                    override fun configure() {
                        bind<SessionFactory>().toInstance(sessionFactory)
                        bind<GCPPubSubClient>().toInstance(gcpPubSubClient)
                    }
                }
            )

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
}

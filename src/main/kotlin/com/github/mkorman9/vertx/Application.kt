package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.tools.firestore.FirestoreInitializer
import com.github.mkorman9.vertx.tools.gcp.GCPPubSubClient
import com.github.mkorman9.vertx.utils.BootstrapUtils
import com.github.mkorman9.vertx.utils.ConfigReader
import com.google.cloud.firestore.Firestore
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import kotlin.system.exitProcess

class Application {
    companion object {
        private val log = LoggerFactory.getLogger(Application::class.java)

        const val PACKAGE_NAME = "com.github.mkorman9.vertx"
    }

    private lateinit var firestore: Firestore
    private lateinit var gcpPubSubClient: GCPPubSubClient

    fun bootstrap(vertx: Vertx) {
        try {
            val config = ConfigReader.read(vertx)

            firestore = FirestoreInitializer.initialize(config)
            gcpPubSubClient = GCPPubSubClient.create(vertx, config)

            BootstrapUtils.bootstrap(
                packageName = PACKAGE_NAME,
                vertx = vertx,
                config = config,
                module = object : KotlinModule() {
                    override fun configure() {
                        bind<Firestore>().toInstance(firestore)
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
        firestore.close()

        log.info("App has been stopped")
    }
}

package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.common.Services
import com.github.mkorman9.vertx.common.getVerticlesToDeploy
import com.github.mkorman9.vertx.tools.aws.sqs.SQSClient
import com.github.mkorman9.vertx.tools.hibernate.HibernateInitializer
import com.github.mkorman9.vertx.tools.postgres.LiquibaseExecutor
import com.github.mkorman9.vertx.utils.BootstrapUtils
import com.github.mkorman9.vertx.utils.Config
import com.github.mkorman9.vertx.utils.ConfigReader
import com.github.mkorman9.vertx.utils.ShutdownHook
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.micrometer.MicrometerMetricsOptions
import io.vertx.micrometer.VertxPrometheusOptions
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import kotlin.system.exitProcess

class Application {
    companion object {
        private val log = LoggerFactory.getLogger(Application::class.java)
    }

    private lateinit var vertx: Vertx
    private lateinit var sessionFactory: SessionFactory
    private lateinit var sqsClient: SQSClient

    fun bootstrap() {
        try {
            createVertx()

            val config = ConfigReader.read(vertx)

            migrateDbSchema(config)

            val services = createServices(config)

            BootstrapUtils.bootstrap(
                vertx = vertx,
                config = config,
                verticles = getVerticlesToDeploy(services)
            )

            log.info("App has been bootstrapped successfully")
        } catch (e: Exception) {
            log.error("Failed to bootstrap the app", e)
            exitProcess(1)
        }

        ShutdownHook.register {
            shutdown()
        }
    }

    private fun shutdown() {
        vertx.close()
            .toCompletionStage()
            .toCompletableFuture()
            .join()

        sqsClient.close()
        sessionFactory.close()

        log.info("App has been stopped")
    }

    private fun createVertx() {
        vertx = Vertx.vertx(
            VertxOptions()
                .setPreferNativeTransport(true)
                .setMetricsOptions(
                    MicrometerMetricsOptions()
                        .setPrometheusOptions(VertxPrometheusOptions().setEnabled(true))
                        .setEnabled(true)
                )
        )
    }

    private fun createServices(config: Config): Services {
        sessionFactory = HibernateInitializer.initialize(vertx, config)
            .toCompletionStage()
            .toCompletableFuture()
            .join()

        sqsClient = SQSClient.create(config)

        return Services.create(sessionFactory, sqsClient)
    }

    private fun migrateDbSchema(config: Config) {
        LiquibaseExecutor.migrateSchema(vertx, config)
            .toCompletionStage()
            .toCompletableFuture()
            .join()
    }
}

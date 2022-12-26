package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.common.Services
import com.github.mkorman9.vertx.common.getVerticlesToDeploy
import com.github.mkorman9.vertx.tools.aws.sqs.SQSClient
import com.github.mkorman9.vertx.tools.hibernate.HibernateInitializer
import com.github.mkorman9.vertx.tools.postgres.LiquibaseExecutor
import com.github.mkorman9.vertx.utils.*
import com.github.mkorman9.vertx.utils.bootstrap.BootstrapUtils
import com.github.mkorman9.vertx.utils.core.Config
import com.github.mkorman9.vertx.utils.core.ConfigReader
import com.github.mkorman9.vertx.utils.core.DeploymentInfo
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.micrometer.MicrometerMetricsOptions
import io.vertx.micrometer.VertxPrometheusOptions
import kotlin.system.exitProcess

object Application {
    private val log = LoggerFactory.getLogger(Application::class.java)

    fun bootstrap() {
        try {
            val vertx = createVertx()
            val config = ConfigReader.read(vertx)

            migrateDbSchema(vertx, config)

            val services = createServices(vertx, config)

            BootstrapUtils.bootstrap(
                vertx = vertx,
                config = config,
                verticles = getVerticlesToDeploy(services)
            )

            log.info("App has been bootstrapped successfully (version: ${DeploymentInfo.get().version})")

            ShutdownHook.register {
                shutdown(vertx, services)
            }
        } catch (e: Exception) {
            log.error("Failed to bootstrap the app", e)
            exitProcess(1)
        }
    }

    private fun shutdown(vertx: Vertx, services: Services) {
        vertx.close()
            .toCompletionStage()
            .toCompletableFuture()
            .join()

        services.sqsClient.close()
        services.sessionFactory.close()

        log.info("App has been stopped")
    }

    private fun createVertx(): Vertx {
        return Vertx.vertx(
            VertxOptions()
                .setPreferNativeTransport(true)
                .setMetricsOptions(
                    MicrometerMetricsOptions()
                        .setPrometheusOptions(VertxPrometheusOptions().setEnabled(true))
                        .setEnabled(true)
                )
        )
    }

    private fun createServices(vertx: Vertx, config: Config): Services {
        val sessionFactory = HibernateInitializer.initialize(vertx, config)
            .toCompletionStage()
            .toCompletableFuture()
            .join()

        val sqsClient = SQSClient.create(config)

        return Services.create(sessionFactory, sqsClient)
    }

    private fun migrateDbSchema(vertx: Vertx, config: Config) {
        LiquibaseExecutor.migrateSchema(vertx, config)
            .toCompletionStage()
            .toCompletableFuture()
            .join()
    }
}

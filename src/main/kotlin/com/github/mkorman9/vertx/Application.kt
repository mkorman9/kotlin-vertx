package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.common.Services
import com.github.mkorman9.vertx.common.getVerticlesToDeploy
import com.github.mkorman9.vertx.tools.aws.sqs.SQSClient
import com.github.mkorman9.vertx.tools.hibernate.HibernateInitializer
import com.github.mkorman9.vertx.tools.postgres.LiquibaseExecutor
import com.github.mkorman9.vertx.utils.BootstrapUtils
import com.github.mkorman9.vertx.utils.ConfigReader
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import kotlin.system.exitProcess

class Application {
    companion object {
        private val log = LoggerFactory.getLogger(Application::class.java)
    }

    private lateinit var sessionFactory: SessionFactory
    private lateinit var sqsClient: SQSClient

    fun bootstrap(vertx: Vertx) {
        try {
            val config = ConfigReader.read(vertx)

            LiquibaseExecutor.migrateSchema(vertx, config)

            sessionFactory = HibernateInitializer.initialize(vertx, config)
                .toCompletionStage()
                .toCompletableFuture()
                .join()

            sqsClient = SQSClient.create(config)

            val services = Services.create(sessionFactory, sqsClient)

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
    }

    fun shutdown() {
        sqsClient.close()
        sessionFactory.close()

        log.info("App has been stopped")
    }
}

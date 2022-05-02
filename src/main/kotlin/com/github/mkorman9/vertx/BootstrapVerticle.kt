package com.github.mkorman9.vertx

import io.vertx.core.Future
import io.vertx.core.impl.launcher.commands.RunCommand
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import org.hibernate.reactive.mutiny.Mutiny
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator
import java.io.IOException
import java.time.LocalDateTime
import java.util.jar.Manifest
import javax.persistence.Persistence
import javax.validation.Validation
import javax.validation.Validator

class BootstrapVerticle : CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(BootstrapVerticle::class.java)

    override suspend fun start() {
        try {
            val config = readConfig().await()
            val sessionFactory = startHibernate(config).await()
            val validator = createBeanValidator()
            val version = readVersionFromManifest().await()
            val appContext = AppContext(
                vertx = vertx,
                config = config,
                sessionFactory = sessionFactory,
                validator = validator,
                version = version,
                startupTime = LocalDateTime.now()
            )

            log.info("BootstrapVerticle has been deployed successfully")

            vertx.deployVerticle(HttpServerVerticle(appContext))
        } catch (e: Exception) {
            log.error("Failed to deploy BootstrapVerticle", e)
            throw e
        }
    }

    private fun readConfig(): Future<Config> {
        return vertx
            .fileSystem()
            .readFile(System.getenv().getOrDefault("CONFIG_FILE", "./config.yml"))
            .map { parseConfig(it) }
    }

    private fun startHibernate(config: Config): Future<Mutiny.SessionFactory> {
        val props = mapOf(
            "javax.persistence.jdbc.url" to config.db.uri,
            "javax.persistence.jdbc.user" to config.db.user,
            "javax.persistence.jdbc.password" to config.db.password
        )

        return vertx
            .executeBlocking { call ->
                val sessionFactory = Persistence
                    .createEntityManagerFactory("default", props)
                    .unwrap(Mutiny.SessionFactory::class.java)

                call.complete(sessionFactory)
            }
    }

    private fun createBeanValidator(): Validator {
        return Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(ParameterMessageInterpolator())
            .buildValidatorFactory()
            .validator
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

package com.github.mkorman9.vertx

import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.core.impl.launcher.commands.RunCommand
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.kotlin.core.http.httpServerOptionsOf
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

class MainVerticle : CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(MainVerticle::class.java)

    override suspend fun start() {
        log.info("Starting MainVerticle...")

        configureJsonCodec()

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

            startHttpServer(appContext).await()

            log.info("MainVerticle started successfully")
        } catch (e: Exception) {
            log.error("Failed to start MainVerticle", e)
            throw e
        }
    }

    private fun configureJsonCodec() {
        val objectMapper = DatabindCodec.mapper()
        objectMapper.registerModule(KotlinModule.Builder().build())
        objectMapper.registerModule(JavaTimeModule())

        objectMapper.dateFormat = StdDateFormat()
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

    private fun startHttpServer(context: AppContext): Future<HttpServer> {
        val mainRouter = MainRouter(context)

        return vertx
            .createHttpServer(
                httpServerOptionsOf(
                    host = context.config.server?.host ?: "0.0.0.0",
                    port = context.config.server?.port ?: 8080,
                    logActivity = true
                )
            )
            .requestHandler { mainRouter.router.handle(it) }
            .listen()
    }
}

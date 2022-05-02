package com.github.mkorman9.vertx

import com.google.inject.Guice
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.impl.launcher.commands.RunCommand
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import org.hibernate.reactive.mutiny.Mutiny
import java.io.IOException
import java.time.LocalDateTime
import java.util.jar.Manifest
import javax.persistence.Persistence

class BootstrapVerticle : CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(BootstrapVerticle::class.java)

    companion object {
        lateinit var cachedContext: AppContext
    }

    override suspend fun start() {
        try {
            val configRetriever = createConfigRetriever()
            val config = configRetriever.config.await()
            val sessionFactory = startHibernate(config).await()
            val version = readVersionFromManifest().await()

            val module = AppModule(configRetriever, sessionFactory)
            val injector = Guice.createInjector(module)

            cachedContext = AppContext(
                vertx = vertx,
                injector = injector,
                version = version,
                startupTime = LocalDateTime.now()
            )

            log.info("BootstrapVerticle has been deployed successfully")

            vertx.deployVerticle(HttpServerVerticle::class.java.name, DeploymentOptions()
                .setInstances(config.getJsonObject("server")?.getInteger("instances") ?: 1)
            )
        } catch (e: Exception) {
            log.error("Failed to deploy BootstrapVerticle", e)
            throw e
        }
    }

    private fun createConfigRetriever(): ConfigRetriever {
        val store = ConfigStoreOptions()
            .setType("file")
            .setFormat("yaml")
            .setConfig(JsonObject()
                .put("path", System.getenv().getOrDefault("CONFIG_FILE", "./config.yml"))
            )

        return ConfigRetriever.create(vertx, ConfigRetrieverOptions().addStore(store))
    }

    private fun startHibernate(config: JsonObject): Future<Mutiny.SessionFactory> {
        val uri = config.getJsonObject("db")?.getString("uri")
            ?: throw RuntimeException("db.uri is missing from config")
        val user = config.getJsonObject("db")?.getString("user")
            ?: throw RuntimeException("db.user is missing from config")
        val password = config.getJsonObject("db")?.getString("password")
            ?: throw RuntimeException("db.password is missing from config")

        val props = mapOf(
            "javax.persistence.jdbc.url" to uri,
            "javax.persistence.jdbc.user" to user,
            "javax.persistence.jdbc.password" to password
        )

        return vertx
            .executeBlocking { call ->
                val sessionFactory = Persistence
                    .createEntityManagerFactory("default", props)
                    .unwrap(Mutiny.SessionFactory::class.java)

                call.complete(sessionFactory)
            }
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

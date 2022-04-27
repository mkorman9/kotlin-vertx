package com.github.mkorman9.vertx

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.core.http.httpServerOptionsOf
import org.hibernate.reactive.mutiny.Mutiny

import javax.persistence.Persistence

class MainVerticle : AbstractVerticle() {
    private val log = LoggerFactory.getLogger(MainVerticle::class.java)

    override fun start(startPromise: Promise<Void>) {
        log.info("Starting main verticle...")

        readConfig()
            .onSuccess { config ->
                startHibernate(config)
                    .onSuccess { sessionFactory ->
                        val appContext = AppContext(
                            vertx = vertx,
                            config = config,
                            sessionFactory = sessionFactory
                        )

                        startHttpServer(appContext)
                            .onSuccess { startPromise.complete() }
                            .onFailure {
                                log.error("Failed to start HTTP server: $it")
                                startPromise.fail(it)
                            }
                    }
                    .onFailure {
                        log.error("Failed to start Hibernate: $it")
                        startPromise.fail(it)
                    }
            }
            .onFailure {
                log.error("Failed to read configuration file: $it")
                startPromise.fail(it)
            }
    }

    private fun readConfig(): Future<Config> {
        val promise = Promise.promise<Config>()

        vertx
            .fileSystem()
            .readFile(System.getenv().getOrDefault("CONFIG_FILE", "./config.yml"))
            .onSuccess {
                try {
                    promise.complete(parseConfig(it))
                } catch (e: Exception) {
                    promise.fail(e)
                }
            }
            .onFailure { promise.fail(it) }

        return promise.future()
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

    private fun startHttpServer(context: AppContext): Future<Void> {
        val promise = Promise.promise<Void>()
        val mainRouter = MainRouter(context)

        vertx
            .createHttpServer(
                httpServerOptionsOf(
                    host = context.config.server?.host ?: "0.0.0.0",
                    port = context.config.server?.port ?: 8080,
                    logActivity = true
                )
            )
            .requestHandler { mainRouter.handle(it) }
            .listen { result ->
                if (result.succeeded()) {
                    promise.complete()
                    log.info("HTTP server started successfully")
                } else {
                    promise.fail(result.cause())
                }
            }

        return promise.future()
    }
}

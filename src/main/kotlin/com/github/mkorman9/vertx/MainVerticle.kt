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

        startHibernate()
            .onSuccess { sessionFactory ->
                val appRouter = AppRouter(
                    vertx,
                    sessionFactory
                )

                startHttpServer(appRouter)
                    .onSuccess { startPromise.complete() }
                    .onFailure {
                        log.error("Failed to start HTTP server")
                        startPromise.fail(it)
                    }
            }
            .onFailure {
                log.error("Failed to start Hibernate")
                startPromise.fail(it)
            }
    }

    private fun startHibernate(): Future<Mutiny.SessionFactory> {
        val props = mapOf(
            "javax.persistence.jdbc.url" to "jdbc:postgresql://localhost:5432/tsexpress",
            "javax.persistence.jdbc.user" to "username",
            "javax.persistence.jdbc.password" to "password"
        )

        return vertx
            .executeBlocking { call ->
                val sessionFactory = Persistence
                    .createEntityManagerFactory("default", props)
                    .unwrap(Mutiny.SessionFactory::class.java)

                call.complete(sessionFactory)
            }
    }

    private fun startHttpServer(appRouter: AppRouter): Future<Void> {
        val promise = Promise.promise<Void>()

        vertx
            .createHttpServer(
                httpServerOptionsOf(
                    port = 8080,
                    logActivity = true
                )
            )
            .requestHandler { appRouter.handle(it) }
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

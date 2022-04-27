package com.github.mkorman9.vertx

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.core.http.httpServerOptionsOf
import org.hibernate.reactive.mutiny.Mutiny

import javax.persistence.Persistence

class MainVerticle : AbstractVerticle() {
    private val log = LoggerFactory.getLogger(MainVerticle::class.java)

    override fun start(startPromise: Promise<Void>) {
        log.info("Starting main verticle...")

        vertx
            .executeBlocking<Mutiny.SessionFactory> { call ->
                val sessionFactory = startHibernate()
                call.complete(sessionFactory)
            }
            .onSuccess { sessionFactory ->
                val appRouter = AppRouter(
                    vertx,
                    sessionFactory
                )

                startHttpServer(appRouter, startPromise)
            }
            .onFailure { handler ->
                log.error("Failed to start Hibernate: ${handler.cause}")
            }
    }

    private fun startHibernate(): Mutiny.SessionFactory {
        val props = mapOf(
            "javax.persistence.jdbc.url" to "jdbc:postgresql://localhost:5432/tsexpress",
            "javax.persistence.jdbc.user" to "username",
            "javax.persistence.jdbc.password" to "password"
        )

        return Persistence
            .createEntityManagerFactory("default", props)
            .unwrap(Mutiny.SessionFactory::class.java)
    }

    private fun startHttpServer(appRouter: AppRouter, donePromise: Promise<Void>) {
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
                    donePromise.complete()
                    log.info("HTTP server started successfully")
                } else {
                    donePromise.fail(result.cause())
                }
            }
    }
}

package com.github.mkorman9.vertx

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.http.httpServerOptionsOf
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient

class MainVerticle : AbstractVerticle() {
    private val log = LoggerFactory.getLogger(MainVerticle::class.java)

    private val sqlClient = createPgClient()
    private val clientRepository = ClientRepository(sqlClient)

    private val router = Router.router(vertx).apply {
        get("/").handler { ctx ->
            ctx.response().endWithJson(StatusDTO(
                status = "OK"
            ))
        }

        get("/clients").handler { ctx ->
            clientRepository.findClients()
                .onSuccess { data -> ctx.response().endWithJson(data) }
                .onFailure { handler -> ctx.fail(500, handler.cause) }
        }
    }

    override fun start(startPromise: Promise<Void>) {
        log.info("Starting main verticle...")

        vertx
            .createHttpServer(
                httpServerOptionsOf(
                    port = 8080,
                    logActivity = true
                )
            )
            .requestHandler { router.handle(it) }
            .listen { result ->
                if (result.succeeded()) {
                    startPromise.complete()
                    log.info("HTTP server started successfully")
                } else {
                    startPromise.fail(result.cause())
                }
            }
    }

    private fun createPgClient(): SqlClient {
        val poolOptions = PoolOptions()
            .setMaxSize(5)

        return PgPool.client(vertx, "postgres://username:password@localhost:5432/tsexpress?sslmode=disable", poolOptions)
    }
}

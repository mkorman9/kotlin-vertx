package com.github.mkorman9.vertx

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.http.httpServerOptionsOf

class MainVerticle : AbstractVerticle() {
    private val log = LoggerFactory.getLogger(MainVerticle::class.java)

    private val router = Router.router(vertx).apply {
        get("/").handler { ctx ->
            ctx.response().endWithJson(StatusDTO(
                status = "OK"
            ))
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
}

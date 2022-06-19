package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.utils.*
import io.vertx.core.http.HttpServer
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.core.http.httpServerOptionsOf
import io.vertx.kotlin.coroutines.await

@DeployVerticle(scalingStrategy = VerticesScalingStrategy.NUM_OF_CPUS, minInstances = 4)
class HttpServerVerticle : ContextualVerticle() {
    companion object {
        private val log = LoggerFactory.getLogger(HttpServerVerticle::class.java)
    }

    private lateinit var server: HttpServer

    override suspend fun start() {
        try {
            val restApi = RestApi(context)

            server = vertx
                .createHttpServer(
                    httpServerOptionsOf(
                        host = config.get<String>("server.host") ?: "0.0.0.0",
                        port = config.get<Int>("server.port") ?: 8080,
                        tcpFastOpen = true,
                        tcpCork = true,
                        reusePort = true
                    )
                )
                .requestHandler { restApi.router.handle(it) }
                .listen()
                .await()
        } catch (e: Exception) {
            log.error("Failed to deploy HttpServerVerticle", e)
            throw e
        }
    }

    override suspend fun stop() {
        server.close().await()
    }
}

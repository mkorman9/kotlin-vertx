package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.utils.DeployVerticle
import com.github.mkorman9.vertx.utils.NUM_OF_CPUS
import com.google.inject.Injector
import io.vertx.core.http.HttpServer
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.core.http.httpServerOptionsOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

@DeployVerticle(instances = NUM_OF_CPUS)
class HttpServerVerticle(
    private val injector: Injector
): CoroutineVerticle() {
    companion object {
        private val log = LoggerFactory.getLogger(HttpServerVerticle::class.java)
    }

    private lateinit var server: HttpServer

    override suspend fun start() {
        try {
            val restApi = RestApi(vertx, this, injector)

            server = vertx
                .createHttpServer(
                    httpServerOptionsOf(
                        host = config.getJsonObject("server")?.getString("host") ?: "0.0.0.0",
                        port = config.getJsonObject("server")?.getInteger("port") ?: 8080,
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

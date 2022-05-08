package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientEventsPublisher
import com.github.mkorman9.vertx.utils.JsonCodecConfig
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.config.ConfigRetriever
import io.vertx.core.http.HttpServer
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.core.http.httpServerOptionsOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

class HttpServerVerticle(
    passedContext: AppContext? = null
): CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(HttpServerVerticle::class.java)

    private val context: AppContext = passedContext ?: BootstrapVerticle.cachedContext
    private lateinit var server: HttpServer

    override suspend fun start() {
        try {
            val configRetriever = context.injector.getInstance<ConfigRetriever>()
            val config = configRetriever.config.await()

            val api = Api(context)
            val websocketHandler = WebsocketHandler(context)

            server = vertx
                .createHttpServer(
                    httpServerOptionsOf(
                        host = config.getJsonObject("server")?.getString("host") ?: "0.0.0.0",
                        port = config.getJsonObject("server")?.getInteger("port") ?: 8080
                    )
                )
                .requestHandler { api.router.handle(it) }
                .webSocketHandler { websocketHandler.handle(it) }
                .listen()
                .await()

            log.info("HttpServerVerticle has been deployed")
        } catch (e: Exception) {
            log.error("Failed to deploy HttpServerVerticle", e)
            throw e
        }
    }

    override suspend fun stop() {
        server.close().await()
        log.info("HttpServerVerticle has been stopped")
    }

    companion object {
        init {
            JsonCodecConfig()
        }
    }
}

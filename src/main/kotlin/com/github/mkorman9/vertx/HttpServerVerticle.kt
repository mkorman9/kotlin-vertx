package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.utils.DeployVerticle
import com.github.mkorman9.vertx.utils.JsonCodecConfig
import com.google.inject.Injector
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.config.ConfigRetriever
import io.vertx.core.http.HttpServer
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.core.http.httpServerOptionsOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

@DeployVerticle(configKey = "server")
class HttpServerVerticle(
    passedInjector: Injector? = null
): CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(HttpServerVerticle::class.java)

    private val injector: Injector = passedInjector ?: BootstrapVerticle.injector
    private lateinit var server: HttpServer

    override suspend fun start() {
        try {
            val configRetriever = injector.getInstance<ConfigRetriever>()
            val config = configRetriever.config.await()

            val apiRouter = Api(injector).createRouter()
            val websocketApi = WebsocketApi(injector)

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
                .requestHandler { apiRouter.handle(it) }
                .webSocketHandler { websocketApi.handle(it) }
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

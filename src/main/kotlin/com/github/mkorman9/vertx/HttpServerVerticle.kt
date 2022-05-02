package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.utils.JsonCodecConfig
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
        val configRetriever = context.injector.getInstance(ConfigRetriever::class.java)
        val config = configRetriever.config.await()

        val mainRouter = MainRouter(context)

        server = vertx
            .createHttpServer(
                httpServerOptionsOf(
                    host = config.getJsonObject("server")?.getString("host") ?: "0.0.0.0",
                    port = config.getJsonObject("server")?.getInteger("port") ?: 8080
                )
            )
            .requestHandler { mainRouter.router.handle(it) }
            .listen()
            .await()

        log.info("HttpServerVerticle has been deployed successfully")
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

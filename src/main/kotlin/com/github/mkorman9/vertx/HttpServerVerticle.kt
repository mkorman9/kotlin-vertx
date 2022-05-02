package com.github.mkorman9.vertx

import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.vertx.config.ConfigRetriever
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.kotlin.core.http.httpServerOptionsOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

class HttpServerVerticle(
    private val context: AppContext
): CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(HttpServerVerticle::class.java)

    override suspend fun start() {
        configureJsonCodec()

        val configRetriever = context.injector.getInstance(ConfigRetriever::class.java)
        val config = configRetriever.config.await()

        val mainRouter = MainRouter(context)

        vertx
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

    private fun configureJsonCodec() {
        val objectMapper = DatabindCodec.mapper()
        objectMapper.registerModule(KotlinModule.Builder().build())
        objectMapper.registerModule(JavaTimeModule())

        objectMapper.dateFormat = StdDateFormat()
    }
}

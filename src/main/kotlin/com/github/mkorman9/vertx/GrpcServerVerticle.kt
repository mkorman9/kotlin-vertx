package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientServiceGrpcImpl
import com.github.mkorman9.vertx.utils.DeployVerticle
import com.google.inject.Injector
import dev.misfitlabs.kotlinguice4.getInstance
import io.grpc.Server
import io.grpc.ServerBuilder
import io.vertx.config.ConfigRetriever
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

@DeployVerticle
class GrpcServerVerticle(
    private val injector: Injector
) : CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(GrpcServerVerticle::class.java)

    private lateinit var server: Server

    override suspend fun start() {
        try {
            val configRetriever = injector.getInstance<ConfigRetriever>()
            val config = configRetriever.config.await()

            val port = config.getJsonObject("grpc")?.getInteger("port") ?: 9090

            val clientsService = injector.getInstance<ClientServiceGrpcImpl>()

            vertx.executeBlocking<Void> { call ->
                server = ServerBuilder
                    .forPort(port)
                    .addService(clientsService)
                    .build()
                    .start()
                call.complete()
            }.await()

            log.info("GrpcServerVerticle has been deployed")
        } catch (e: Exception) {
            log.error("Failed to deploy GrpcServerVerticle", e)
        }
    }

    override suspend fun stop() {
        vertx.executeBlocking<Void> { call ->
            server.shutdown().awaitTermination()
            call.complete()
        }.await()

        log.info("GrpcServerVerticle has been stopped")
    }
}

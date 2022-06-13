package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientServiceGrpcImpl
import com.github.mkorman9.vertx.utils.DeployVerticle
import com.google.inject.Injector
import dev.misfitlabs.kotlinguice4.getInstance
import io.grpc.Server
import io.grpc.ServerBuilder
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

@DeployVerticle
class GrpcServerVerticle(
    private val injector: Injector
) : CoroutineVerticle() {
    companion object {
        private val log = LoggerFactory.getLogger(GrpcServerVerticle::class.java)
    }

    private lateinit var server: Server

    override suspend fun start() {
        try {
            val clientsService = injector.getInstance<ClientServiceGrpcImpl>()

            vertx.executeBlocking<Void> { call ->
                server = ServerBuilder
                    .forPort(config.getJsonObject("grpc")?.getInteger("port") ?: 9090)
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

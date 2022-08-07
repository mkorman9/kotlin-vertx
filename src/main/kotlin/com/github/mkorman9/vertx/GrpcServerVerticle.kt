package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientServiceGrpcImpl
import com.github.mkorman9.vertx.common.Services
import com.github.mkorman9.vertx.utils.verticleContext
import com.github.mkorman9.vertx.utils.get
import io.grpc.Server
import io.grpc.ServerBuilder
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

class GrpcServerVerticle(
    private val services: Services
) : CoroutineVerticle() {
    companion object {
        private val log = LoggerFactory.getLogger(GrpcServerVerticle::class.java)
    }

    private lateinit var server: Server

    override suspend fun start() {
        try {
            vertx.executeBlocking<Void> { call ->
                server = ServerBuilder
                    .forPort(config.get<Int>("GRPC_PORT") ?: 9090)
                    .addService(ClientServiceGrpcImpl(services, verticleContext))
                    .build()
                    .start()
                call.complete()
            }.await()
        } catch (e: Exception) {
            log.error("Failed to deploy GrpcServerVerticle", e)
            throw e
        }
    }

    override suspend fun stop() {
        vertx.executeBlocking<Void> { call ->
            server.shutdown().awaitTermination()
            call.complete()
        }.await()
    }
}

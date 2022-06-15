package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientServiceGrpcImpl
import com.github.mkorman9.vertx.utils.ContextualVerticle
import com.github.mkorman9.vertx.utils.DeployVerticle
import com.github.mkorman9.vertx.utils.get
import io.grpc.Server
import io.grpc.ServerBuilder
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.coroutines.await

@DeployVerticle
class GrpcServerVerticle : ContextualVerticle() {
    companion object {
        private val log = LoggerFactory.getLogger(GrpcServerVerticle::class.java)
    }

    private lateinit var server: Server

    override suspend fun start() {
        try {
            vertx.executeBlocking<Void> { call ->
                server = ServerBuilder
                    .forPort(config.get<Int>("grpc.port") ?: 9090)
                    .addService(ClientServiceGrpcImpl(context))
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

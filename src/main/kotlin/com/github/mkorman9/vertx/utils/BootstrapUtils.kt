package com.github.mkorman9.vertx.utils

import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory

object BootstrapUtils {
    private val log = LoggerFactory.getLogger(BootstrapUtils::class.java)

    init {
        JsonCodec.configure()
    }

    fun bootstrap(vertx: Vertx, config: Config, verticles: List<VerticleDefinition>) {
        DeploymentInfo.initialize(config)

        val futures = verticles
            .flatMap { definition ->
                val futures = mutableListOf<Future<*>>()
                var verticleName = "UnknownVerticle"

                for (i in 0 until definition.instances) {
                    val instance = definition.create()
                    verticleName = instance.javaClass.name

                    val f = vertx.deployVerticle(
                        instance,
                        DeploymentOptions()
                            .setConfig(config)
                            .setWorker(definition.worker)
                            .setWorkerPoolName(definition.workerPoolName.ifEmpty { null })
                            .setWorkerPoolSize(definition.workerPoolSize)
                    )

                    futures.add(f)
                }

                log.info("Deployed ${definition.instances} instances of verticle $verticleName")

                futures
            }

        CompositeFuture.all(futures)
            .toCompletionStage()
            .toCompletableFuture()
            .join()
    }
}

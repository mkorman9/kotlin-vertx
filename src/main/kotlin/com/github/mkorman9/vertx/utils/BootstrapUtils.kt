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

    fun bootstrap(vertx: Vertx, config: Config, verticleDefinitions: List<VerticleDefinition>) {
        val futures = verticleDefinitions
            .flatMap { definition ->
                val futures = mutableListOf<Future<*>>()

                for (i in 0 until definition.instances) {
                    val f = vertx.deployVerticle(
                        definition.create(),
                        DeploymentOptions()
                            .setConfig(config)
                            .setWorker(definition.worker)
                            .setWorkerPoolName(definition.workerPoolName.ifEmpty { null })
                            .setWorkerPoolSize(definition.workerPoolSize)
                    )

                    futures.add(f)
                }

                log.info("Deployed ${definition.instances} instances of ${definition.name}")

                futures
            }

        CompositeFuture.all(futures)
            .toCompletionStage()
            .toCompletableFuture()
            .join()
    }
}

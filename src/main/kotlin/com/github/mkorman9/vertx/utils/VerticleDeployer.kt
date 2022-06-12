package com.github.mkorman9.vertx.utils

import io.vertx.core.*

class VerticleDeployer {
    companion object {
        fun scanAndDeploy(vertx: Vertx, packageName: String, vararg constructorParams: Any) {
            val futures = mutableListOf<Future<*>>()

            ReflectionsUtils.findClasses(packageName, DeployVerticle::class.java)
                .forEach { c ->
                    val annotation = c.annotations.filterIsInstance<DeployVerticle>()
                        .first()

                    var instances = annotation.instances
                    if (instances == NUM_OF_CPUS) {
                        instances = Runtime.getRuntime().availableProcessors()
                    }

                    for (i in 0 until instances) {
                        val future = vertx.deployVerticle(
                            c.declaredConstructors[0].newInstance(*constructorParams) as Verticle,
                            DeploymentOptions()
                                .setWorker(annotation.worker)
                                .setWorkerPoolName(annotation.workerPoolName.ifEmpty { null })
                                .setWorkerPoolSize(annotation.workerPoolSize)
                        )
                        futures.add(future)
                    }
                }

            CompositeFuture.all(futures)
                .toCompletionStage()
                .toCompletableFuture()
                .join()
        }
    }
}

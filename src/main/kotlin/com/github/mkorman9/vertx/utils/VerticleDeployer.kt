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

                    val future = vertx.deployVerticle(
                        c.declaredConstructors[0].newInstance(*constructorParams) as Verticle,
                        DeploymentOptions()
                            .setWorker(annotation.worker)
                            .setWorkerPoolName(annotation.workerPoolName.ifEmpty { null })
                            .setWorkerPoolSize(annotation.workerPoolSize)
                    )
                    futures.add(future)
                }

            CompositeFuture.all(futures)
                .toCompletionStage()
                .toCompletableFuture()
                .join()
        }
    }
}

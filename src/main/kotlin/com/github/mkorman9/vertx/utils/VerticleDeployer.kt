package com.github.mkorman9.vertx.utils

import com.google.inject.Injector
import io.vertx.core.*
import io.vertx.core.impl.logging.LoggerFactory
import kotlin.math.ceil

class VerticleDeployer {
    companion object {
        private val log = LoggerFactory.getLogger(VerticleDeployer::class.java)

        fun scanAndDeploy(vertx: Vertx, packageName: String, config: Config, injector: Injector) {
            val futures = mutableListOf<Future<*>>()

            ReflectionsUtils.findClasses(packageName, DeployVerticle::class.java)
                .forEach { c ->
                    val annotation = c.annotations.filterIsInstance<DeployVerticle>()
                        .first()
                    val instances = parseInstancesNumber(annotation.instances)

                    for (i in 0 until instances) {
                        val instance = c.declaredConstructors[0].newInstance() as Verticle
                        if (instance is ContextualVerticle) {
                            instance.injector = injector
                        }

                        val future = vertx.deployVerticle(
                            instance,
                            DeploymentOptions()
                                .setConfig(config)
                                .setWorker(annotation.worker)
                                .setWorkerPoolName(annotation.workerPoolName.ifEmpty { null })
                                .setWorkerPoolSize(annotation.workerPoolSize)
                        )
                        futures.add(future)
                    }

                    log.info("Deployed $instances instances of ${c.simpleName}")
                }

            CompositeFuture.all(futures)
                .toCompletionStage()
                .toCompletableFuture()
                .join()
        }

        private fun parseInstancesNumber(instances: Int): Int {
            return when(instances) {
                NUM_OF_CPUS -> Runtime.getRuntime().availableProcessors()
                HALF_NUM_OF_CPUS -> ceil(Runtime.getRuntime().availableProcessors() / 2.0).toInt()
                TWICE_NUM_OF_CPUS -> Runtime.getRuntime().availableProcessors() * 2
                else -> instances
            }
        }
    }
}

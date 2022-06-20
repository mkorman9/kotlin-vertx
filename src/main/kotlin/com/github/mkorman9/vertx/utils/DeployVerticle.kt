package com.github.mkorman9.vertx.utils

import io.vertx.core.VertxOptions

enum class VerticesScalingStrategy {
    CONSTANT,
    NUM_OF_CPUS
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DeployVerticle(
    val scalingStrategy: VerticesScalingStrategy = VerticesScalingStrategy.CONSTANT,
    val minInstances: Int = 1,
    val worker: Boolean = false,
    val workerPoolName: String = "",
    val workerPoolSize: Int = VertxOptions.DEFAULT_WORKER_POOL_SIZE
)

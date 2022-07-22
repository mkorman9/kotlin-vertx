package com.github.mkorman9.vertx.utils

import io.vertx.core.Verticle
import io.vertx.core.VertxOptions

data class VerticleDefinition(
    val create: () -> Verticle,
    val instances: Int = 1,
    val worker: Boolean = false,
    val workerPoolName: String = "",
    val workerPoolSize: Int = VertxOptions.DEFAULT_WORKER_POOL_SIZE
)

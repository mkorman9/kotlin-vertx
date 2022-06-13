package com.github.mkorman9.vertx.utils

import io.vertx.core.VertxOptions

const val NUM_OF_CPUS = -1
const val HALF_NUM_OF_CPUS = -2
const val TWICE_NUM_OF_CPUS = -3

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DeployVerticle(
    val instances: Int = 1,
    val worker: Boolean = false,
    val workerPoolName: String = "",
    val workerPoolSize: Int = VertxOptions.DEFAULT_WORKER_POOL_SIZE
)

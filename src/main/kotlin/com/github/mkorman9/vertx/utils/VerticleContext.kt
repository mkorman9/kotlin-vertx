package com.github.mkorman9.vertx.utils

import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineScope

data class VerticleContext(
    val vertx: Vertx,
    val config: Config,
    val scope: CoroutineScope
)

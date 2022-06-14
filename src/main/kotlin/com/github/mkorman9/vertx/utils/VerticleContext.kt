package com.github.mkorman9.vertx.utils

import com.google.inject.Injector
import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineScope

data class VerticleContext(
    val vertx: Vertx,
    val scope: CoroutineScope,
    val injector: Injector
)

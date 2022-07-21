package com.github.mkorman9.vertx.utils

import io.vertx.kotlin.coroutines.CoroutineVerticle

abstract class ContextualVerticle : CoroutineVerticle() {
    protected val context: VerticleContext by lazy {
        VerticleContext(
            vertx = vertx,
            config = config,
            scope = this
        )
    }
}

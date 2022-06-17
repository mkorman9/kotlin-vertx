package com.github.mkorman9.vertx.utils

import com.google.inject.Injector
import io.vertx.kotlin.coroutines.CoroutineVerticle

abstract class ContextualVerticle : CoroutineVerticle() {
    lateinit var injector: Injector

    val context: VerticleContext by lazy {
        VerticleContext(
            vertx = vertx,
            config = config,
            scope = this,
            injector = injector
        )
    }
}

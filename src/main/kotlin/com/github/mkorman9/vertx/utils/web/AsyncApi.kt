package com.github.mkorman9.vertx.utils.web

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

abstract class AsyncApi(vertx: Vertx) : CoroutineScope {
    override val coroutineContext: CoroutineContext by lazy { vertx.dispatcher() + SupervisorJob() }

    @Suppress("LeakingThis")
    protected val scope: CoroutineScope = this
}

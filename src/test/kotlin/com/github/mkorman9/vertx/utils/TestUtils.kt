package com.github.mkorman9.vertx.utils

import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun coroutineTest(vertx: Vertx, testContext: VertxTestContext, testBody: suspend () -> Unit) {
    GlobalScope.launch(vertx.dispatcher()) {
        try {
            testBody()
            testContext.completeNow()
        } catch (t: Throwable) {
            testContext.failNow(t)
        }
    }
}

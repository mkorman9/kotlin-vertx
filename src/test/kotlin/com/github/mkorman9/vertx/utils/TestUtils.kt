package com.github.mkorman9.vertx.utils

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.util.Modules
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun createTestInjector(packageName: String, module: KotlinModule): Injector {
    return Guice.createInjector(
        Modules.override(TestModuleBase(packageName)).with(module)
    )
}

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

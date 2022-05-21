package com.github.mkorman9.vertx

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.util.Modules
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime

fun createTestInjector(vertx: Vertx, module: KotlinModule): Injector {
    val context = DeploymentContext(
        version = "test",
        startupTime = LocalDateTime.now(),
        environment = "default"
    )

    return Guice.createInjector(Modules.override(TestModuleBase(vertx, context)).with(module))
}

fun asyncTest(vertx: Vertx, testContext: VertxTestContext, testBody: suspend () -> Unit) {
    GlobalScope.launch(vertx.dispatcher()) {
        try {
            testBody()
            testContext.completeNow()
        } catch (t: Throwable) {
            testContext.failNow(t)
        }
    }
}

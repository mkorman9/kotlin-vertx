package com.github.mkorman9.vertx

import com.google.inject.Guice
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.core.Vertx
import java.time.LocalDateTime

fun createTestAppContext(vertx: Vertx, module: KotlinModule): AppContext {
    return AppContext(
        vertx = vertx,
        injector = Guice.createInjector(TestModuleBase(vertx), module),
        version = "test",
        startupTime = LocalDateTime.now()
    )
}

package com.github.mkorman9.vertx

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.util.Modules
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.core.Vertx
import java.time.LocalDateTime

fun createTestInjector(vertx: Vertx, module: KotlinModule): Injector {
    val context = AppContext(
        vertx = vertx,
        version = "test",
        startupTime = LocalDateTime.now()
    )

    return Guice.createInjector(Modules.override(TestModuleBase(context)).with(module))
}

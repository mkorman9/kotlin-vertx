package com.github.mkorman9.vertx

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.util.Modules
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.core.Vertx
import java.time.LocalDateTime

fun createTestInjector(vertx: Vertx, module: KotlinModule): Injector {
    val context = DeploymentContext(
        version = "test",
        startupTime = LocalDateTime.now(),
        environment = "default"
    )

    return Guice.createInjector(Modules.override(TestModuleBase(vertx, context)).with(module))
}

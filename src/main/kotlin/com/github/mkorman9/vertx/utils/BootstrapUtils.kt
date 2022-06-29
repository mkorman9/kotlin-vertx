package com.github.mkorman9.vertx.utils

import io.vertx.core.Vertx

object BootstrapUtils {
    init {
        JsonCodec.configure()
    }

    fun bootstrap(packageName: String, vertx: Vertx, config: Config, module: com.google.inject.Module) {
        val injector = InjectorUtils.createInjector(packageName, module)

        VerticleDeployer.scanAndDeploy(vertx, packageName, config, injector)
    }
}

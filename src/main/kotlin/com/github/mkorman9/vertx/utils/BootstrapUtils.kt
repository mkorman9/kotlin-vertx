package com.github.mkorman9.vertx.utils

import com.google.inject.util.Modules
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.core.Vertx

object BootstrapUtils {
    init {
        JsonCodec.configure()
    }

    fun bootstrap(packageName: String, vertx: Vertx, config: Config, module: com.google.inject.Module) {
        val baseModule = object : KotlinModule() {
            override fun configure() {
                bind<Config>().toInstance(config)
            }
        }

        val injector = InjectorUtils.createInjector(
            packageName,
            Modules.override(baseModule).with(module)
        )

        VerticleDeployer.scanAndDeploy(vertx, packageName, config, injector)
    }
}

package com.github.mkorman9.vertx.utils

import com.google.inject.util.Modules
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.core.Vertx

class BootstrapUtils {
    companion object {
        fun bootstrap(packageName: String, vertx: Vertx, config: Config, module: com.google.inject.Module) {
            JsonCodec.configure()

            val vertxModule = object : KotlinModule() {
                override fun configure() {
                    bind<Config>().toInstance(config)
                }
            }
            val injector = InjectorUtils.createInjector(
                packageName,
                Modules.override(vertxModule).with(module)
            )

            VerticleDeployer.scanAndDeploy(vertx, packageName, config, injector)
        }
    }
}

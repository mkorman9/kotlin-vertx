package com.github.mkorman9.vertx.utils

import com.google.inject.util.Modules
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.core.Vertx

class BootstrapUtils {
    companion object {
        fun bootstrap(packageName: String, vertx: Vertx, appModule: com.google.inject.Module) {
            JsonCodec.configure()

            val vertxModule = object : KotlinModule() {
                override fun configure() {
                    bind<Vertx>().toInstance(vertx)
                }
            }
            val injector = InjectorUtils.createInjector(
                packageName,
                Modules.override(vertxModule).with(appModule)
            )

            VerticleDeployer.scanAndDeploy(vertx, packageName, injector)
        }
    }
}

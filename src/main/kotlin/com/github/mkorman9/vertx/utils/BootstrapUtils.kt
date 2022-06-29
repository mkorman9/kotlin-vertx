package com.github.mkorman9.vertx.utils

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Singleton
import com.google.inject.util.Modules
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.core.Vertx

object BootstrapUtils {
    init {
        JsonCodec.configure()
    }

    fun bootstrap(packageName: String, vertx: Vertx, config: Config, module: com.google.inject.Module) {
        val injector = createInjector(packageName, module)

        VerticleDeployer.scanAndDeploy(vertx, packageName, config, injector)
    }

    private fun createInjector(packageName: String, module: com.google.inject.Module): Injector {
        return Guice.createInjector(
            Modules.override(createAutoconfiguredModule(packageName))
                .with(module)
        )
    }

    private fun createAutoconfiguredModule(packageName: String): KotlinModule {
        return object : KotlinModule() {
            override fun configure() {
                ReflectionsUtils.findClasses(packageName, Singleton::class.java)
                    .forEach { c ->
                        bind(c)

                        c.genericInterfaces.forEach { i ->
                            @Suppress("UNCHECKED_CAST")
                            bind(Class.forName(i.typeName) as Class<Any>).to(c)
                        }
                    }
            }
        }
    }
}

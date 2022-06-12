package com.github.mkorman9.vertx.utils

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Singleton
import com.google.inject.util.Modules
import dev.misfitlabs.kotlinguice4.KotlinModule

class InjectorUtils {
    companion object {
        fun createInjector(packageName: String, appModule: KotlinModule): Injector {
            return Guice.createInjector(
                Modules.override(scanForBeans(packageName))
                    .with(appModule)
            )
        }

        private fun scanForBeans(packageName: String): KotlinModule {
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
}
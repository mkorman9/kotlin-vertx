package com.github.mkorman9.vertx.utils

import com.github.mkorman9.vertx.Application
import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.mockk.mockkClass
import io.vertx.core.Vertx
import kotlin.jvm.internal.Reflection

class TestModuleBase(
    private val vertx: Vertx,
    private val config: Config
) : KotlinModule() {
    companion object {
        init {
            JsonCodec.configure()
        }
    }

    override fun configure() {
        bind<Vertx>().toInstance(vertx)
        bind<Config>().toInstance(config)

        ReflectionsUtils.findClasses(Application.PACKAGE_NAME, Singleton::class.java)
            .forEach {
                val kclass = Reflection.createKotlinClass(Class.forName(it.name))
                bind(it).toInstance(mockkClass(kclass, relaxed = true))
            }
    }
}

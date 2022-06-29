package com.github.mkorman9.vertx.utils

import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.mockk.mockkClass
import kotlin.jvm.internal.Reflection

class TestModuleBase(
    private val packageName: String
) : KotlinModule() {
    companion object {
        init {
            JsonCodec.configure()
        }
    }

    override fun configure() {
        ReflectionsUtils.findClasses(packageName, Singleton::class.java)
            .forEach {
                val kclass = Reflection.createKotlinClass(Class.forName(it.name))
                bind(it).toInstance(mockkClass(kclass, relaxed = true))
            }
    }
}

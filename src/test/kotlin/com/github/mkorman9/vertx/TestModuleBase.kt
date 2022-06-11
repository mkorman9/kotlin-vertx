package com.github.mkorman9.vertx

import dev.misfitlabs.kotlinguice4.KotlinModule
import io.mockk.mockk
import io.mockk.mockkClass
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import kotlin.jvm.internal.Reflection

class TestModuleBase(
    private val vertx: Vertx,
    private val context: DeploymentContext
) : KotlinModule() {
    override fun configure() {
        bind<Vertx>().toInstance(vertx)
        bind<DeploymentContext>().toInstance(context)
        bind<SessionFactory>().toInstance(mockk())
        bind<Config>().toInstance(createConfig())

        AppModule.getInjectableClasses()
            .forEach {
                val kclass = Reflection.createKotlinClass(Class.forName(it.name))
                bind(it).toInstance(mockkClass(kclass, relaxed = true))
            }
    }

    private fun createConfig(): Config {
        return JsonObject()
            .put("host", "127.0.0.1")
            .put("port", 8080)
    }
}

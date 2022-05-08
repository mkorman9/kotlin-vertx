package com.github.mkorman9.vertx

import dev.misfitlabs.kotlinguice4.KotlinModule
import io.mockk.mockk
import io.mockk.mockkClass
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.rabbitmq.RabbitMQClient
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
        bind<RabbitMQClient>().toInstance(mockk())
        bind<ConfigRetriever>().toInstance(createConfigRetriever())

        AppModule.getInjectableClasses()
            .forEach {
                val kclass = Reflection.createKotlinClass(Class.forName(it.name))
                bind(it).toInstance(mockkClass(kclass, relaxed = true))
            }
    }

    private fun createConfigRetriever(): ConfigRetriever {
        val config = JsonObject().apply {
            put(
                "server",
                JsonObject()
                    .put("host", "127.0.0.1")
                    .put("port", 8080)
            )
        }

        val store = ConfigStoreOptions()
            .setType("json")
            .setConfig(config)
        return ConfigRetriever.create(vertx, ConfigRetrieverOptions().addStore(store))
    }
}

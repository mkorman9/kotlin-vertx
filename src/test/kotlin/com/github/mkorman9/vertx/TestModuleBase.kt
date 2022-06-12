package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.utils.Config
import com.github.mkorman9.vertx.utils.JsonCodec
import com.github.mkorman9.vertx.utils.ReflectionsUtils
import com.github.mkorman9.vertx.utils.gcp.GCPPubSubClient
import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.mockk.mockk
import io.mockk.mockkClass
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import kotlin.jvm.internal.Reflection

class TestModuleBase(
    private val vertx: Vertx
) : KotlinModule() {
    companion object {
        init {
            JsonCodec.configure()
        }
    }

    override fun configure() {
        bind<Vertx>().toInstance(vertx)
        bind<SessionFactory>().toInstance(mockk())
        bind<GCPPubSubClient>().toInstance(mockk())
        bind<Config>().toInstance(createConfig())

        ReflectionsUtils.findClasses(Application.PACKAGE_NAME, Singleton::class.java)
            .forEach {
                val kclass = Reflection.createKotlinClass(Class.forName(it.name))
                bind(it).toInstance(mockkClass(kclass, relaxed = true))
            }
    }

    private fun createConfig(): Config {
        return JsonObject()
            .put("server",
                JsonObject()
                    .put("host", "127.0.0.1")
                    .put("port", 8080)
            )
    }
}

package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.utils.Config
import com.github.mkorman9.vertx.utils.ReflectionsUtils
import com.github.mkorman9.vertx.utils.gcp.GCPPubSubClient
import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.core.Vertx
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory

class AppModule(
    private val vertx: Vertx,
    private val config: Config,
    private val sessionFactory: SessionFactory,
    private val gcpPubSubClient: GCPPubSubClient
) : KotlinModule() {
    companion object {
        const val packageName = "com.github.mkorman9.vertx"

        fun getInjectableClasses(): List<Class<Any>> {
            return ReflectionsUtils.findClasses(packageName, Singleton::class.java)
        }
    }

    override fun configure() {
        bind<Vertx>().toInstance(vertx)
        bind<Config>().toInstance(config)
        bind<SessionFactory>().toInstance(sessionFactory)
        bind<GCPPubSubClient>().toInstance(gcpPubSubClient)

        getInjectableClasses()
            .forEach { c ->
                bind(c)

                c.genericInterfaces.forEach { i ->
                    @Suppress("UNCHECKED_CAST")
                    bind(Class.forName(i.typeName) as Class<Any>).to(c)
                }
            }
    }
}

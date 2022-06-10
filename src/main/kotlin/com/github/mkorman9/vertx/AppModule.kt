package com.github.mkorman9.vertx

import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.config.ConfigRetriever
import io.vertx.core.Vertx
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import org.reflections.Reflections

class AppModule(
    private val vertx: Vertx,
    private val context: DeploymentContext,
    private val configRetriever: ConfigRetriever,
    private val sessionFactory: SessionFactory
) : KotlinModule() {
    companion object {
        const val packageName = "com.github.mkorman9.vertx"

        fun getInjectableClasses(): Set<Class<Any>> {
            val packageReflections = Reflections(packageName)

            @Suppress("UNCHECKED_CAST")
            return packageReflections.getTypesAnnotatedWith(Singleton::class.java) as Set<Class<Any>>
        }
    }

    override fun configure() {
        bind<Vertx>().toInstance(vertx)
        bind<DeploymentContext>().toInstance(context)
        bind<ConfigRetriever>().toInstance(configRetriever)
        bind<SessionFactory>().toInstance(sessionFactory)

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

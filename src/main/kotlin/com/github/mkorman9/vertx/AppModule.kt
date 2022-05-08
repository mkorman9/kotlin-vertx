package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.security.AuthorizationMiddleware
import com.github.mkorman9.vertx.security.AuthorizationMiddlewareImpl
import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.config.ConfigRetriever
import io.vertx.rabbitmq.RabbitMQClient
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import org.reflections.Reflections

class AppModule(
    private val configRetriever: ConfigRetriever,
    private val sessionFactory: SessionFactory,
    private val rabbitMQClient: RabbitMQClient
) : KotlinModule() {
    companion object {
        private const val packageName = "com.github.mkorman9.vertx"

        fun getInjectableClasses(): Set<Class<Any>> {
            val packageReflections = Reflections(packageName)

            @Suppress("UNCHECKED_CAST")
            return packageReflections.getTypesAnnotatedWith(Singleton::class.java) as Set<Class<Any>>
        }
    }

    override fun configure() {
        bind<ConfigRetriever>().toInstance(configRetriever)
        bind<SessionFactory>().toInstance(sessionFactory)
        bind<RabbitMQClient>().toInstance(rabbitMQClient)

        getInjectableClasses()
            .forEach { bind(it) }

        bind<AuthorizationMiddleware>().to(AuthorizationMiddlewareImpl::class.java)
    }
}

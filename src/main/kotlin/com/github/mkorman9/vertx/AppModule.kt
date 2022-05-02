package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientRepository
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.config.ConfigRetriever
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import javax.validation.Validator

class AppModule(
    private val sessionFactory: SessionFactory,
    private val validator: Validator,
    private val configRetriever: ConfigRetriever
) : KotlinModule() {
    override fun configure() {
        bind<SessionFactory>().toInstance(sessionFactory)
        bind<Validator>().toInstance(validator)
        bind<ConfigRetriever>().toInstance(configRetriever)

        bind<ClientRepository>().toInstance(ClientRepository(sessionFactory))
    }
}

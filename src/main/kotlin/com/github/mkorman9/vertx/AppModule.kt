package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientRepository
import com.github.mkorman9.vertx.security.AccountRepository
import com.github.mkorman9.vertx.security.SessionRepository
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.config.ConfigRetriever
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory

class AppModule(
    private val configRetriever: ConfigRetriever,
    private val sessionFactory: SessionFactory
) : KotlinModule() {
    override fun configure() {
        bind<ConfigRetriever>().toInstance(configRetriever)
        bind<SessionFactory>().toInstance(sessionFactory)

        bind<ClientRepository>().toInstance(ClientRepository(sessionFactory))
        bind<AccountRepository>().toInstance(AccountRepository(sessionFactory))
        bind<SessionRepository>().toInstance(SessionRepository(sessionFactory))
    }
}

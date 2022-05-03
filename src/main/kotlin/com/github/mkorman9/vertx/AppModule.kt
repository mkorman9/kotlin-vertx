package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientRepository
import com.github.mkorman9.vertx.security.AccountRepository
import com.github.mkorman9.vertx.security.AuthorizationMiddleware
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

        val clientRepository = ClientRepository(sessionFactory)
        val accountRepository = AccountRepository(sessionFactory)
        val sessionRepository = SessionRepository(sessionFactory)

        bind<ClientRepository>().toInstance(clientRepository)
        bind<AccountRepository>().toInstance(accountRepository)
        bind<SessionRepository>().toInstance(sessionRepository)

        bind<AuthorizationMiddleware>().toInstance(AuthorizationMiddleware(sessionRepository))
    }
}

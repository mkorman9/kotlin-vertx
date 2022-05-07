package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.client.ClientEventsPublisher
import com.github.mkorman9.vertx.client.ClientRepository
import com.github.mkorman9.vertx.security.AccountRepository
import com.github.mkorman9.vertx.security.AuthorizationMiddleware
import com.github.mkorman9.vertx.security.AuthorizationMiddlewareImpl
import com.github.mkorman9.vertx.security.SessionRepository
import com.github.mkorman9.vertx.utils.AdvisoryLock
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.config.ConfigRetriever
import io.vertx.rabbitmq.RabbitMQClient
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory

class AppModule(
    private val configRetriever: ConfigRetriever,
    private val sessionFactory: SessionFactory,
    private val rabbitMQClient: RabbitMQClient
) : KotlinModule() {
    override fun configure() {
        bind<ConfigRetriever>().toInstance(configRetriever)
        bind<SessionFactory>().toInstance(sessionFactory)
        bind<RabbitMQClient>().toInstance(rabbitMQClient)
        bind<AdvisoryLock>().toInstance(AdvisoryLock(sessionFactory))

        val clientRepository = ClientRepository(sessionFactory)
        val accountRepository = AccountRepository(sessionFactory)
        val sessionRepository = SessionRepository(sessionFactory)

        bind<ClientRepository>().toInstance(clientRepository)
        bind<AccountRepository>().toInstance(accountRepository)
        bind<SessionRepository>().toInstance(sessionRepository)

        bind<AuthorizationMiddleware>().toInstance(AuthorizationMiddlewareImpl(sessionRepository))

        bind<ClientEventsPublisher>().toInstance(ClientEventsPublisher(rabbitMQClient))
    }
}

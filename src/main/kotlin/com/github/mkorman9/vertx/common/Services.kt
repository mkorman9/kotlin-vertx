package com.github.mkorman9.vertx.common

import com.github.mkorman9.vertx.client.ClientEventsPublisher
import com.github.mkorman9.vertx.client.ClientRepository
import com.github.mkorman9.vertx.security.AccountRepository
import com.github.mkorman9.vertx.security.AuthorizationMiddleware
import com.github.mkorman9.vertx.security.AuthorizationMiddlewareImpl
import com.github.mkorman9.vertx.security.SessionRepository
import com.github.mkorman9.vertx.tools.aws.sqs.SQSClient
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory

data class Services(
    val sessionFactory: SessionFactory,
    val sqsClient: SQSClient,
    val sessionRepository: SessionRepository,
    val clientEventsPublisher: ClientEventsPublisher,
    val clientRepository: ClientRepository,
    val accountRepository: AccountRepository,
    val authorizationMiddleware: AuthorizationMiddleware
) {
    companion object {
        fun create(sessionFactory: SessionFactory, sqsClient: SQSClient): Services {
            val sessionRepository = SessionRepository(sessionFactory)
            val clientEventsPublisher = ClientEventsPublisher()
            val clientRepository = ClientRepository(sessionFactory)
            val accountRepository = AccountRepository(sessionFactory)
            val authorizationMiddleware = AuthorizationMiddlewareImpl(sessionRepository)

            return Services(
                sessionFactory = sessionFactory,
                sqsClient = sqsClient,
                sessionRepository = sessionRepository,
                clientEventsPublisher = clientEventsPublisher,
                clientRepository = clientRepository,
                accountRepository = accountRepository,
                authorizationMiddleware = authorizationMiddleware
            )
        }
    }
}

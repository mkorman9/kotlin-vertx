package com.github.mkorman9.vertx.client

import io.vertx.core.Future
import io.vertx.core.Promise
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory

class ClientRepository(
    private val sessionFactory: SessionFactory
) {

    fun findClients(): Future<List<Client>> {
        val promise = Promise.promise<List<Client>>()

        sessionFactory
            .withSession { session ->
                session.createQuery("from Client", Client::class.java).resultList
            }
            .subscribe().with(
                { promise.complete(it) },
                { promise.fail(it) }
            )

        return promise.future()
    }
}

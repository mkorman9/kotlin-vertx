package com.github.mkorman9.vertx

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
                { item -> promise.complete(item) },
                { failure -> promise.fail(failure) }
            )

        return promise.future()
    }
}

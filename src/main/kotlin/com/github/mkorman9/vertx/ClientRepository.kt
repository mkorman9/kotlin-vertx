package com.github.mkorman9.vertx

import io.smallrye.mutiny.Uni
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory

class ClientRepository(
    private val sessionFactory: SessionFactory
) {

    fun findClients(): Uni<List<Client>> {
        return sessionFactory.withSession { session ->
            session.createQuery("from Client", Client::class.java).resultList
        }
    }
}

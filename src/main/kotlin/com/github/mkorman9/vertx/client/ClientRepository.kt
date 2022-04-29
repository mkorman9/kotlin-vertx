package com.github.mkorman9.vertx.client

import io.vertx.core.Future
import io.vertx.core.Promise
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import java.util.*

class ClientRepository(
    private val sessionFactory: SessionFactory
) {

    fun findAll(): Future<List<Client>> {
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

    fun findById(id: String): Future<Client?> {
        val promise = Promise.promise<Client?>()
        val idUUID = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            promise.complete(null)
            return promise.future()
        }

        sessionFactory
            .withSession { session ->
                session.find(Client::class.java, idUUID)
            }
            .subscribe().with(
                { promise.complete(it) },
                { promise.fail(it) }
            )

        return promise.future()
    }

    fun add(client: Client): Future<Void> {
        val promise = Promise.promise<Void>()

        sessionFactory
            .withSession { session ->
                session.withTransaction {
                    session.persist(client)
                }
            }
            .subscribe().with(
                { promise.complete() },
                { promise.fail(it) }
            )

        return promise.future()
    }
}

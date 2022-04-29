package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.utils.withSession
import com.github.mkorman9.vertx.utils.withTransaction
import io.vertx.core.Future
import io.vertx.core.Promise
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import java.util.*

class ClientRepository(
    private val sessionFactory: SessionFactory
) {

    fun findAll(): Future<List<Client>> {
        return withSession(sessionFactory) { session ->
            session.createQuery("from Client", Client::class.java).resultList
        }
    }

    fun findById(id: String): Future<Client?> {
        val promise = Promise.promise<Client?>()
        val idUUID = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            promise.complete(null)
            return promise.future()
        }

        return withSession(sessionFactory) { session ->
            session.find(Client::class.java, idUUID)
        }
    }

    fun add(client: Client): Future<Void> {
        return withTransaction(sessionFactory) { session, _ ->
            session.persist(client)
        }
    }
}

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
            session.createQuery("from Client c where c.deleted = false", Client::class.java).resultList
        }
    }

    fun findById(id: String): Future<Client?> {
        val idUUID = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            return Future.succeededFuture(null)
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

    fun update(id: String, payload: ClientUpdatePayload): Future<Boolean> {
        val idUUID = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            return Future.succeededFuture(false)
        }

        return withTransaction(sessionFactory) { session, _ ->
            session.find(Client::class.java, idUUID)
                .onItem().ifNotNull().transform { client ->
                    if (payload.gender != null) {
                        client.gender = payload.gender
                    }
                    if (payload.firstName != null) {
                        client.firstName = payload.firstName
                    }
                    if (payload.lastName != null) {
                        client.lastName = payload.lastName
                    }
                    if (payload.address != null) {
                        client.address = payload.address
                    }
                    if (payload.phoneNumber != null) {
                        client.phoneNumber = payload.phoneNumber
                    }
                    if (payload.email != null) {
                        client.email = payload.email
                    }
                    if (payload.birthDate != null) {
                        client.birthDate = payload.birthDate
                    }
                    if (payload.creditCards != null) {
                        client.creditCards.removeIf { cc1 ->
                            !payload.creditCards.any { cc2 -> cc1.number == cc2.number }
                        }

                        payload.creditCards.forEach { cc1 ->
                            if(!client.creditCards.any { cc2 -> cc1.number == cc2.number }) {
                                client.creditCards.add(
                                    CreditCard(
                                        clientId = idUUID,
                                        number = cc1.number
                                    )
                                )
                            }
                        }
                    }

                    session.merge(client)
                    true
                }
                .onItem().ifNull().continueWith(false)
        }
    }
}

package com.github.mkorman9.vertx.client

import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.core.Future
import io.vertx.core.Vertx
import java.time.ZoneOffset
import java.util.*

@Singleton
class ClientRepository @Inject constructor(
    private val vertx: Vertx,
    private val firestore: Firestore
) {
    companion object {
        private const val CLIENTS_COLLECTION = "clients"
    }

    fun findAll(): Future<List<Client>> {
        return vertx.executeBlocking { call ->
            val docs = firestore.collection(CLIENTS_COLLECTION)
                .get()
                .get()

            call.complete(docs.toObjects(Client::class.java))
        }
    }

    fun findPaged(
        filtering: ClientsFilteringOptions,
        paging: ClientsPagingOptions,
        sorting: ClientsSortingOptions
    ): Future<List<Client>> {
        return vertx.executeBlocking { call ->
            val collection = firestore.collection(CLIENTS_COLLECTION)
            var query = createQueryWithFilters(collection, filtering)
            query = addPagingToQuery(query, paging)
            query = addSortingToQuery(query, sorting)

            val docs = query
                .get()
                .get()
            val clients = docs.toObjects(Client::class.java)

            call.complete(clients)
        }
    }

    fun findById(id: String): Future<Client?> {
        return vertx.executeBlocking { call ->
            val doc = firestore.collection(CLIENTS_COLLECTION)
                .document(id)
                .get()
                .get()

            val client = doc.toObject(Client::class.java)
            if (client == null || client.deleted) {
                call.complete(null)
            } else {
                call.complete(client)
            }
        }
    }

    fun add(payload: ClientAddPayload): Future<Client> {
        val id = UUID.randomUUID().toString()
        val client = Client(
            id = id,
            gender = payload.gender ?: "-",
            firstName = payload.firstName,
            lastName = payload.lastName,
            address = payload.address ?: "",
            phoneNumber = payload.phoneNumber ?: "",
            email = payload.email ?: "",
            birthDate = payload.birthDate?.toEpochSecond(ZoneOffset.UTC),
            creditCards = (payload.creditCards ?: listOf()).map { it.number }.toMutableList()
        )

        return vertx.executeBlocking { call ->
            firestore.collection(CLIENTS_COLLECTION)
                .document(id)
                .set(client)
                .get()

            call.complete(client)
        }
    }

    fun update(id: String, payload: ClientUpdatePayload): Future<Client?> {
        return vertx.executeBlocking { call ->
            val doc = firestore.collection(CLIENTS_COLLECTION)
                .document(id)
                .get()
                .get()

            val client = doc.toObject(Client::class.java)
            if (client == null || client.deleted) {
                call.complete(null)
            } else {
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
                    client.birthDate = payload.birthDate.toEpochSecond(ZoneOffset.UTC)
                }
                if (payload.creditCards != null) {
                    client.creditCards = payload.creditCards.map { it.number }
                }

                firestore.collection(CLIENTS_COLLECTION)
                    .document(id)
                    .set(client)
                    .get()

                call.complete(client)
            }
        }
    }

    fun delete(id: String): Future<Boolean> {
        return vertx.executeBlocking { call ->
            val doc = firestore.collection(CLIENTS_COLLECTION)
                .document(id)
                .get()
                .get()

            val client = doc.toObject(Client::class.java)
            if (client == null || client.deleted) {
                call.complete(false)
            } else {
                client.deleted = true

                firestore.collection(CLIENTS_COLLECTION)
                    .document(id)
                    .set(client)
                    .get()

                call.complete(true)
            }
        }
    }

    private fun createQueryWithFilters(collection: CollectionReference, filtering: ClientsFilteringOptions): Query {
        var query = collection.whereEqualTo("deleted", false)

        if (filtering.gender != null) {
            query = query.whereEqualTo("gender", filtering.gender)
        }

        if (filtering.firstName != null) {
            query = query.whereEqualTo("firstName", filtering.firstName)
        }

        if (filtering.lastName != null) {
            query = query.whereEqualTo("lastName", filtering.lastName)
        }

        if (filtering.address != null) {
            query = query.whereEqualTo("address", filtering.address)
        }

        if (filtering.phoneNumber != null) {
            query = query.whereEqualTo("phoneNumber", filtering.phoneNumber)
        }

        if (filtering.email != null) {
            query = query.whereEqualTo("email", filtering.email)
        }

        if (filtering.bornAfter != null) {
            query = query.whereGreaterThanOrEqualTo("birthDate", filtering.bornAfter.toEpochSecond(ZoneOffset.UTC))
        }

        if (filtering.bornBefore != null) {
            query = query.whereLessThan("birthDate", filtering.bornBefore.toEpochSecond(ZoneOffset.UTC))
        }

        if (filtering.creditCard != null) {
            query = query.whereArrayContains("creditCards", filtering.creditCard)
        }

        return query
    }

    private fun addSortingToQuery(query: Query, sorting: ClientsSortingOptions): Query {
        return query.orderBy(
            sorting.sortBy,
            if (sorting.sortReverse) Query.Direction.DESCENDING else Query.Direction.ASCENDING
        )
    }

    private fun addPagingToQuery(query: Query, paging: ClientsPagingOptions): Query {
        return query
            .offset((paging.pageNumber - 1) * paging.pageSize)
            .limit(paging.pageSize)
    }
}

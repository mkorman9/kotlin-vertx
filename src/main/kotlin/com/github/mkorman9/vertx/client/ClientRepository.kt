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
    private val firestore: Firestore
) {
    companion object {
        private const val CLIENTS_COLLECTION = "clients"
    }

    fun findAll(vertx: Vertx): Future<List<Client>> {
        return vertx.executeBlocking { call ->
            val docs = firestore.collection(CLIENTS_COLLECTION)
                .get()
                .get()
            val clientDocuments = docs.toObjects(ClientDocument::class.java)

            call.complete(clientDocuments.map { it.toClient() })
        }
    }

    fun findPaged(
        vertx: Vertx,
        filtering: ClientFilteringOptions,
        paging: ClientPagingOptions,
        sorting: ClientSortingOptions
    ): Future<ClientPage> {
        return vertx.executeBlocking { call ->
            val collection = firestore.collection(CLIENTS_COLLECTION)
            var query = createQueryWithFilters(collection, filtering)
            query = addPagingToQuery(query, paging)
            query = addSortingToQuery(query, sorting)

            val docs = query
                .get()
                .get()
            val clientDocuments = docs.toObjects(ClientDocument::class.java)

            val clientPage = ClientPage(
                data = clientDocuments.map { it.toClient() },
                page = paging.pageNumber
            )
            call.complete(clientPage)
        }
    }

    fun findByCursor(
        vertx: Vertx,
        filtering: ClientFilteringOptions,
        cursorOptions: ClientCursorOptions,
    ): Future<ClientCursor> {
        return vertx.executeBlocking { call ->
            val collection = firestore.collection(CLIENTS_COLLECTION)
            var query = createQueryWithFilters(collection, filtering)
            query = query.orderBy("id", Query.Direction.ASCENDING)
                .limit(cursorOptions.limit)

            if (cursorOptions.cursor != null) {
                query = query.whereGreaterThan("id", cursorOptions.cursor)
            }

            val docs = query
                .get()
                .get()
            val clientDocuments = docs.toObjects(ClientDocument::class.java)
            val clients = clientDocuments.map { it.toClient() }

            val cursor = if (clients.isNotEmpty()) {
                ClientCursor(data = clients, nextCursor = clients[clients.size - 1].id.toString())
            } else {
                ClientCursor(data = clients, nextCursor = null)
            }

            call.complete(cursor)
        }
    }

    fun findById(vertx: Vertx, id: String): Future<Client?> {
        return vertx.executeBlocking { call ->
            val doc = firestore.collection(CLIENTS_COLLECTION)
                .document(id)
                .get()
                .get()

            val clientDocument = doc.toObject(ClientDocument::class.java)
            if (clientDocument == null || clientDocument.deleted) {
                call.complete(null)
            } else {
                call.complete(clientDocument.toClient())
            }
        }
    }

    fun add(vertx: Vertx, payload: ClientAddPayload): Future<Client> {
        val doc = firestore
            .collection(CLIENTS_COLLECTION)
            .document()

        val client = Client(
            id = UUID.fromString(doc.id),
            gender = payload.gender ?: "-",
            firstName = payload.firstName,
            lastName = payload.lastName,
            address = payload.address ?: "",
            phoneNumber = payload.phoneNumber ?: "",
            email = payload.email ?: "",
            birthDate = payload.birthDate,
            creditCards = (payload.creditCards ?: listOf()).map { it.number }.toMutableList()
        )

        return vertx.executeBlocking { call ->
            doc
                .set(ClientDocument.fromClient(client))
                .get()

            call.complete(client)
        }
    }

    fun update(vertx: Vertx, id: String, payload: ClientUpdatePayload): Future<Client?> {
        return vertx.executeBlocking { call ->
            val doc = firestore.collection(CLIENTS_COLLECTION)
                .document(id)
                .get()
                .get()

            val clientDocument = doc.toObject(ClientDocument::class.java)
            if (clientDocument == null || clientDocument.deleted) {
                call.complete(null)
            } else {
                val client = clientDocument.toClient()

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
                    client.creditCards = payload.creditCards.map { it.number }.toMutableList()
                }

                firestore.collection(CLIENTS_COLLECTION)
                    .document(id)
                    .set(ClientDocument.fromClient(client))
                    .get()

                call.complete(client)
            }
        }
    }

    fun delete(vertx: Vertx, id: String): Future<Boolean> {
        return vertx.executeBlocking { call ->
            val doc = firestore.collection(CLIENTS_COLLECTION)
                .document(id)
                .get()
                .get()

            val clientDocument = doc.toObject(ClientDocument::class.java)
            if (clientDocument == null || clientDocument.deleted) {
                call.complete(false)
            } else {
                val client = clientDocument.toClient()
                client.deleted = true

                firestore.collection(CLIENTS_COLLECTION)
                    .document(id)
                    .set(ClientDocument.fromClient(client))
                    .get()

                call.complete(true)
            }
        }
    }

    private fun createQueryWithFilters(collection: CollectionReference, filtering: ClientFilteringOptions): Query {
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

    private fun addSortingToQuery(query: Query, sorting: ClientSortingOptions): Query {
        return query.orderBy(
            sorting.sortBy,
            if (sorting.sortReverse) Query.Direction.DESCENDING else Query.Direction.ASCENDING
        )
    }

    private fun addPagingToQuery(query: Query, paging: ClientPagingOptions): Query {
        return query
            .offset((paging.pageNumber - 1) * paging.pageSize)
            .limit(paging.pageSize)
    }
}

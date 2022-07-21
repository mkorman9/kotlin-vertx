package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.tools.hibernate.withSession
import com.github.mkorman9.vertx.tools.hibernate.withTransaction
import io.smallrye.mutiny.Uni
import io.vertx.core.Future
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import java.lang.Integer.max
import java.util.*
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root
import kotlin.math.ceil

class ClientRepository(
    private val sessionFactory: SessionFactory
) {

    fun findAll(): Future<List<Client>> {
        return withSession(sessionFactory) { session ->
            session.createQuery("from Client c where c.deleted = false", Client::class.java).resultList
        }
    }

    fun findPaged(
        filtering: ClientFilteringOptions,
        paging: ClientPagingOptions,
        sorting: ClientSortingOptions
    ): Future<ClientPage> {
        val criteriaBuilder = sessionFactory.criteriaBuilder
        val dataQuery = buildDataQuery(filtering, sorting, criteriaBuilder)
        val countQuery = buildCountQuery(filtering, criteriaBuilder)

        return withSession(sessionFactory) { session ->
            Uni.combine().all().unis(
                session.createQuery(dataQuery)
                    .setFirstResult((paging.pageNumber - 1) * paging.pageSize)
                    .setMaxResults(paging.pageSize)
                    .resultList,
                session.createQuery(countQuery)
                    .singleResult
            )
                .asTuple()
                .onItem().transform { tuple ->
                    ClientPage(
                        data = tuple.item1,
                        page = paging.pageNumber,
                        totalPages = max(1, ceil(tuple.item2.toDouble() / paging.pageSize.toDouble()).toInt())
                    )
                }
        }
    }

    fun findByCursor(
        filtering: ClientFilteringOptions,
        cursorOptions: ClientCursorOptions
    ): Future<ClientCursor> {
        val criteriaBuilder = sessionFactory.criteriaBuilder
        val query = buildCursorQuery(filtering, cursorOptions, criteriaBuilder)

        return withSession(sessionFactory) { session ->
            session.createQuery(query)
                .setMaxResults(cursorOptions.limit)
                .resultList
                .onItem().transform { clients ->
                    var nextCursor: UUID? = null
                    if (clients.isNotEmpty()) {
                        nextCursor = clients[clients.size - 1].id
                    }

                    ClientCursor(
                        data = clients,
                        nextCursor = nextCursor?.toString()
                    )
                }
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
                .onItem().ifNotNull().transform { client ->
                    if (client.deleted) {
                        null
                    } else {
                        client
                    }
                }
        }
    }

    fun add(payload: ClientAddPayload): Future<Client> {
        val id = UUID.randomUUID()

        return withTransaction(sessionFactory) { session, _ ->
            val client = Client(
                id = id,
                gender = payload.gender ?: "-",
                firstName = payload.firstName,
                lastName = payload.lastName,
                address = payload.address,
                phoneNumber = payload.phoneNumber,
                email = payload.email,
                birthDate = payload.birthDate,
                creditCards = (payload.creditCards ?: listOf()).map {
                    CreditCard(
                        clientId = id,
                        number = it.number
                    )
                }.toMutableList()
            )

            session.persist(client)
                .map { client }
        }
    }

    fun update(id: String, payload: ClientUpdatePayload): Future<Client?> {
        val idUUID = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            return Future.succeededFuture(null)
        }

        return withTransaction(sessionFactory) { session, _ ->
            session.find(Client::class.java, idUUID)
                .onItem().ifNotNull().call { client ->
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
                            if (!client.creditCards.any { cc2 -> cc1.number == cc2.number }) {
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
                }
        }
    }

    fun delete(id: String): Future<Boolean> {
        val idUUID = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            return Future.succeededFuture(false)
        }

        return withTransaction(sessionFactory) { session, _ ->
            session.find(Client::class.java, idUUID)
                .onItem().ifNotNull().transformToUni { client ->
                    if (client.deleted) {
                        Uni.createFrom().item(false)
                    } else {
                        client.deleted = true
                        session.merge(client)
                            .map { true }
                    }
                }
                .onItem().ifNull().continueWith(false)
        }
    }

    private fun buildCountQuery(
        filtering: ClientFilteringOptions,
        criteriaBuilder: CriteriaBuilder
    ): CriteriaQuery<Long> {
        val query = criteriaBuilder.createQuery(Long::class.java)
        val root = query.from(Client::class.java)

        val whereClause = buildPredicate(filtering, criteriaBuilder, query, root)

        query.select(criteriaBuilder.count(root))
        query.where(whereClause)

        return query
    }

    private fun buildDataQuery(
        filtering: ClientFilteringOptions,
        sorting: ClientSortingOptions,
        criteriaBuilder: CriteriaBuilder,
    ): CriteriaQuery<Client> {
        val query = criteriaBuilder.createQuery(Client::class.java)
        val root = query.from(Client::class.java)

        val whereClause = buildPredicate(filtering, criteriaBuilder, query, root)

        query.select(root)
        query.where(whereClause)

        if (sorting.sortReverse) {
            query.orderBy(criteriaBuilder.desc(root.get<String>(sorting.sortBy)))
        } else {
            query.orderBy(criteriaBuilder.asc(root.get<String>(sorting.sortBy)))
        }

        return query
    }

    private fun buildCursorQuery(
        filtering: ClientFilteringOptions,
        cursorOptions: ClientCursorOptions,
        criteriaBuilder: CriteriaBuilder,
    ): CriteriaQuery<Client> {
        val query = criteriaBuilder.createQuery(Client::class.java)
        val root = query.from(Client::class.java)

        var whereClause = buildPredicate(filtering, criteriaBuilder, query, root)

        if (cursorOptions.cursor != null) {
            whereClause = criteriaBuilder.and(
                whereClause,
                criteriaBuilder.greaterThan(root.get("id"), cursorOptions.cursor)
            )
        }

        query.select(root)
        query.where(whereClause)
        query.orderBy(criteriaBuilder.asc(root.get<String>("id")))

        return query
    }

    private fun <T> buildPredicate(
        filtering: ClientFilteringOptions,
        criteriaBuilder: CriteriaBuilder,
        query: CriteriaQuery<T>,
        root: Root<Client>
    ): Predicate {
        val predicates = mutableListOf<Predicate>()

        predicates.add(criteriaBuilder.equal(root.get<Boolean>("deleted"), false))

        if (filtering.gender != null) {
            predicates.add(
                criteriaBuilder.equal(
                    root.get<String>("gender"),
                    filtering.gender
                )
            )
        }
        if (filtering.firstName != null) {
            predicates.add(
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("firstName")),
                    "%${filtering.firstName.lowercase()}%"
                )
            )
        }
        if (filtering.lastName != null) {
            predicates.add(
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("lastName")),
                    "%${filtering.lastName.lowercase()}%"
                )
            )
        }
        if (filtering.address != null) {
            predicates.add(
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("address")),
                    "%${filtering.address.lowercase()}%"
                )
            )
        }
        if (filtering.phoneNumber != null) {
            predicates.add(
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("phoneNumber")),
                    "%${filtering.phoneNumber.lowercase()}%"
                )
            )
        }
        if (filtering.email != null) {
            predicates.add(
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("email")),
                    "%${filtering.email.lowercase()}%"
                )
            )
        }
        if (filtering.bornAfter != null) {
            predicates.add(
                criteriaBuilder.greaterThanOrEqualTo(
                    root.get("birthDate"),
                    criteriaBuilder.literal(filtering.bornAfter)
                )
            )
        }
        if (filtering.bornBefore != null) {
            predicates.add(
                criteriaBuilder.lessThan(
                    root.get("birthDate"),
                    criteriaBuilder.literal(filtering.bornBefore)
                )
            )
        }
        if (filtering.creditCard != null) {
            val subquery = query.subquery(String::class.java)
            val creditCardRoot = subquery.from(CreditCard::class.java)

            subquery.select(creditCardRoot.get("clientId"))
            subquery.where(criteriaBuilder.like(creditCardRoot.get("number"), "%${filtering.creditCard}%"))

            predicates.add(root.get<String>("id").`in`(subquery))
        }

        return criteriaBuilder.and(*predicates.toTypedArray())
    }
}

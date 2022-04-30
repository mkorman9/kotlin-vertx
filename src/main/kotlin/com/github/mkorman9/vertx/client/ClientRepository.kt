package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.utils.withSession
import com.github.mkorman9.vertx.utils.withTransaction
import io.smallrye.mutiny.Uni
import io.vertx.core.Future
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
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
        filtering: ClientsFilteringOptions = ClientsFilteringOptions(),
        paging: ClientsPagingOptions = ClientsPagingOptions(),
        sorting: ClientsSortingOptions = ClientsSortingOptions()
    ): Future<ClientsPage> {
        val criteriaBuilder = sessionFactory.criteriaBuilder
        val countQuery = buildCountQuery(filtering, criteriaBuilder)
        val dataQuery = buildDataQuery(filtering, sorting, criteriaBuilder)

        return withSession(sessionFactory) { session ->
            Uni.combine().all().unis(
                session.createQuery(dataQuery)
                    .setFirstResult(paging.pageNumber * paging.pageSize)
                    .setMaxResults(paging.pageSize)
                    .resultList,
                session.createQuery(countQuery)
                    .singleResult
            )
                .asTuple()
                .onItem().transform { tuple ->
                    ClientsPage(
                        data = tuple.item1,
                        page = paging.pageNumber,
                        totalPages = ceil(tuple.item2.toDouble() / paging.pageSize.toDouble()).toInt()
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

    fun add(client: Client): Future<Void> {
        return withTransaction(sessionFactory) { session, _ ->
            session.persist(client)
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
                .onItem().ifNotNull().transform { client ->
                    client.deleted = true
                    session.merge(client)
                    true
                }
                .onItem().ifNull().continueWith(false)
        }
    }

    private fun buildCountQuery(
        filtering: ClientsFilteringOptions,
        criteriaBuilder: CriteriaBuilder
    ): CriteriaQuery<Long> {
        val query = criteriaBuilder.createQuery(Long::class.java)
        val root = query.from(Client::class.java)

        val whereClause = buildPredicate(filtering, criteriaBuilder, root)

        query.select(criteriaBuilder.count(root))
        query.where(whereClause)

        return query
    }

    private fun buildDataQuery(
        filtering: ClientsFilteringOptions,
        sorting: ClientsSortingOptions,
        criteriaBuilder: CriteriaBuilder,
    ): CriteriaQuery<Client> {
        val query = criteriaBuilder.createQuery(Client::class.java)
        val root = query.from(Client::class.java)

        val whereClause = buildPredicate(filtering, criteriaBuilder, root)

        query.select(root)
        query.where(whereClause)

        if (sorting.reverseSort) {
            query.orderBy(criteriaBuilder.desc(root.get<String>(sorting.sortBy)))
        } else {
            query.orderBy(criteriaBuilder.asc(root.get<String>(sorting.sortBy)))
        }

        return query
    }

    private fun buildPredicate(
        filtering: ClientsFilteringOptions,
        criteriaBuilder: CriteriaBuilder,
        root: Root<Client>
    ): Predicate {
        val predicates = mutableListOf<Predicate>()

        predicates.add(criteriaBuilder.equal(root.get<Boolean>("deleted"), false))

        if (filtering.gender != null) {
            predicates.add(criteriaBuilder.equal(root.get<String>("gender"), filtering.gender))
        }
        if (filtering.firstName != null) {
            predicates.add(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), "%${filtering.firstName.lowercase()}%")
            )
        }
        if (filtering.lastName != null) {
            predicates.add(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), "%${filtering.lastName.lowercase()}%")
            )
        }
        if (filtering.address != null) {
            predicates.add(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("address")), "%${filtering.address.lowercase()}%")
            )
        }
        if (filtering.phoneNumber != null) {
            predicates.add(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneNumber")), "%${filtering.phoneNumber.lowercase()}%")
            )
        }
        if (filtering.email != null) {
            predicates.add(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%${filtering.email.lowercase()}%")
            )
        }

        return criteriaBuilder.and(*predicates.toTypedArray())
    }
}

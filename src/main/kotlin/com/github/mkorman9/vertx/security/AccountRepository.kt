package com.github.mkorman9.vertx.security

import com.github.mkorman9.vertx.tools.hibernate.withSession
import com.github.mkorman9.vertx.tools.hibernate.withTransaction
import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.core.Future
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory

@Singleton
class AccountRepository @Inject constructor(
    private val sessionFactory: SessionFactory
) {
    fun findByCredentialsEmail(email: String): Future<Account?> {
        return withSession(sessionFactory) { session ->
            val query = session.createQuery(
                "from Account a where a.credentials.email = :email",
                Account::class.java
            )
            query.setParameter("email", email)

            query.singleResultOrNull
                .onItem().ifNotNull().transform { account ->
                    if (account.deleted) {
                        null
                    } else {
                        account
                    }
                }
        }
    }

    fun add(account: Account): Future<Void> {
        return withTransaction(sessionFactory) { session, _ ->
            session.persist(account)
        }
    }
}

package com.github.mkorman9.vertx.security

import com.github.mkorman9.vertx.utils.withSession
import io.vertx.core.Future
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory

class AccountRepository(
    private val sessionFactory: SessionFactory
) {
    fun findByCredentialsEmail(email: String): Future<Account?> {
        return withSession(sessionFactory) { session ->
            val query = session.createQuery("from Account a where a.credentials.email = :email", Account::class.java)
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
}

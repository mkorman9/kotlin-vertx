package com.github.mkorman9.vertx.security

import com.github.mkorman9.vertx.utils.withTransaction
import io.vertx.core.Future
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory

class SessionRepository(
    private val sessionFactory: SessionFactory
) {
    fun add(sessionObject: Session): Future<Session> {
        return withTransaction(sessionFactory) { session, _ ->
            session.merge(sessionObject)
        }
    }
}

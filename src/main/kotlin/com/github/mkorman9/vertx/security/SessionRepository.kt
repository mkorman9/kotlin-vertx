package com.github.mkorman9.vertx.security

import com.github.mkorman9.vertx.utils.withSession
import com.github.mkorman9.vertx.utils.withTransaction
import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.core.Future
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import java.time.LocalDateTime

@Singleton
class SessionRepository @Inject constructor(
    private val sessionFactory: SessionFactory
) {
    fun findByToken(token: String): Future<Session?> {
        return withSession(sessionFactory) { session ->
            val query = session.createQuery("from Session s where s.token = :token and (s.expiresAt is null or s.expiresAt > current_timestamp())", Session::class.java)
            query.setParameter("token", token)
            query.singleResultOrNull
        }
    }

    fun add(sessionObject: Session): Future<Session> {
        return withTransaction(sessionFactory) { session, _ ->
            session.merge(sessionObject)
        }
    }

    fun refresh(sessionObject: Session): Future<Session> {
        if (sessionObject.duration == null) {
            return Future.succeededFuture(sessionObject)
        }

        return withTransaction(sessionFactory) { session, _ ->
            sessionObject.expiresAt = LocalDateTime.now().plusSeconds(sessionObject.duration!!.toLong())
            session.merge(sessionObject)
        }
    }

    fun delete(sessionObject: Session): Future<Boolean> {
        return withTransaction(sessionFactory) { session, _ ->
            val query = session.createQuery<Void>("delete from Session s where s.id = :id")
            query.setParameter("id", sessionObject.id)

            query.executeUpdate()
                .onItem().ifNotNull().transform { deletedRecords ->
                    deletedRecords > 0
                }
        }
    }

    fun deleteExpired(): Future<Int> {
        return withTransaction(sessionFactory) { session, _ ->
            val query = session.createQuery<Void>("delete from Session s where s.expiresAt < current_timestamp()")
            query.executeUpdate()
        }
    }
}

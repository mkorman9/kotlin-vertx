package com.github.mkorman9.vertx.tools.hibernate

import io.smallrye.mutiny.Uni
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.pgclient.PgException
import org.hibernate.HibernateException
import org.hibernate.reactive.mutiny.Mutiny.Session
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import org.hibernate.reactive.mutiny.Mutiny.Transaction
import javax.persistence.PersistenceException

data class UniqueConstraintViolation(
    val table: String,
    val constraint: String
) : RuntimeException("Duplicate value when inserting to $table, constraint: $constraint")

fun isUniqueConstraintViolation(t: Throwable, constraint: String): Boolean {
    return t is UniqueConstraintViolation && t.constraint == constraint
}

fun <T> withSession(
    sessionFactory: SessionFactory,
    func: java.util.function.Function<Session, Uni<T>>
): Future<T> {
    val promise = Promise.promise<T>()

    sessionFactory
        .withSession { session ->
            func.apply(session)
        }
        .subscribe().with(
            { promise.complete(it) },
            { promise.fail(mapCause(it)) }
        )

    return promise.future()
}

fun <T> withTransaction(
    sessionFactory: SessionFactory,
    func: java.util.function.BiFunction<Session, Transaction, Uni<T>>
): Future<T> {
    val promise = Promise.promise<T>()

    sessionFactory
        .withSession { session ->
            session.withTransaction { transaction ->
                func.apply(session, transaction)
            }
        }
        .subscribe().with(
            { promise.complete(it) },
            { promise.fail(mapCause(it)) }
        )

    return promise.future()
}

private fun mapCause(t: Throwable): Throwable {
    val pgException = findPgException(t) ?: return t

    if (pgException.code == "23505") {  // unique constraint violation
        return UniqueConstraintViolation(table = pgException.table, constraint = pgException.constraint)
    }

    return t
}

private fun findPgException(t: Throwable): PgException? {
    var cause: Throwable? = t

    while(true) {
        if (cause == null) {
            break
        }

        if (cause is PgException) {
            return cause
        }

        cause = cause.cause
    }

    return null
}

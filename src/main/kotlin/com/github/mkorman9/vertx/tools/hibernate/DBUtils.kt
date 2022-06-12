package com.github.mkorman9.vertx.tools.hibernate

import io.smallrye.mutiny.Uni
import io.vertx.core.Future
import io.vertx.core.Promise
import org.hibernate.reactive.mutiny.Mutiny.Session
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import org.hibernate.reactive.mutiny.Mutiny.Transaction

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
            { promise.fail(it) }
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
            { promise.fail(it) }
        )

    return promise.future()
}

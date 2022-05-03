package com.github.mkorman9.vertx.utils

import io.vertx.core.Promise
import io.vertx.core.impl.logging.LoggerFactory
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import java.util.function.Consumer

class AdvisoryLock(
    private val sessionFactory: SessionFactory
) {
    private val log = LoggerFactory.getLogger(AdvisoryLock::class.java)

    fun acquire(lockId: Long, func: Consumer<Promise<Void>>) {
        withSession(sessionFactory) { session ->
            val query = session.createNativeQuery<Boolean>("SELECT pg_try_advisory_lock(:lockId)")
            query.setParameter("lockId", lockId)
            query.singleResultOrNull
        }
            .onSuccess { result ->
                if (result) {
                    log.debug("Acquired advisory lock '${lockId}'")

                    val promise = Promise.promise<Void>()

                    try {
                        func.accept(promise)
                    } catch (t: Throwable) {
                        log.debug("Advisory lock handler for lock '${lockId}' has thrown exception", t)
                    }

                    promise.future().onComplete {
                        withSession(sessionFactory) { session ->
                            val query = session.createNativeQuery<Boolean>("SELECT pg_advisory_unlock(:lockId)")
                            query.setParameter("lockId", lockId)
                            query.singleResultOrNull
                        }
                            .onFailure { failure ->
                                log.debug("Failed to unlock advisory lock '${lockId}'", failure)
                            }
                    }
                } else {
                    log.debug("Failed to acquire advisory lock '${lockId}'")
                }
            }
            .onFailure { failure -> throw failure }
    }
}

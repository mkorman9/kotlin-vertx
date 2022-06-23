package com.github.mkorman9.vertx.tools.hibernate

import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.coroutines.await
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory

@Singleton
class AdvisoryLock @Inject constructor(
    private val sessionFactory: SessionFactory
) {
    companion object {
        private val log = LoggerFactory.getLogger(AdvisoryLock::class.java)
    }

    suspend fun acquire(lockId: Long, f: suspend () -> Unit) {
        val lockAcquired = try {
            withSession(sessionFactory) { session ->
                val query = session.createNativeQuery<Boolean>("SELECT pg_try_advisory_lock(:lockId)")
                query.setParameter("lockId", lockId)
                query.singleResultOrNull
            }.await()
        } catch (e: Exception) {
            log.error("Failed to acquire advisory lock '${lockId}'", e)
            return
        }

        if (lockAcquired) {
            log.debug("Acquired advisory lock '${lockId}'")

            try {
                f()
            } catch (e: Exception) {
                log.error("Advisory lock handler for lock '${lockId}' has thrown exception", e)
            } finally {
                try {
                    withSession(sessionFactory) { session ->
                        val query = session.createNativeQuery<Boolean>("SELECT pg_advisory_unlock(:lockId)")
                        query.setParameter("lockId", lockId)
                        query.singleResultOrNull
                    }.await()

                    log.debug("Released advisory lock '${lockId}'")
                } catch (e: Exception) {
                    log.error("Failed to release advisory lock '${lockId}'", e)
                }
            }
        } else {
            log.debug("Unable to acquire advisory lock '${lockId}'")
        }
    }
}

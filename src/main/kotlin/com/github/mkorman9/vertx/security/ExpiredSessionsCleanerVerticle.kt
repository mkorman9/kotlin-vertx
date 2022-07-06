package com.github.mkorman9.vertx.security

import com.github.mkorman9.vertx.tools.hibernate.AdvisoryLock
import com.github.mkorman9.vertx.utils.ContextualVerticle
import com.github.mkorman9.vertx.utils.DeployVerticle
import com.github.mkorman9.vertx.utils.Scheduler
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.coroutines.await
import java.time.ZoneOffset

@DeployVerticle
class ExpiredSessionsCleanerVerticle : ContextualVerticle() {
    companion object {
        private val log = LoggerFactory.getLogger(ExpiredSessionsCleanerVerticle::class.java)
    }

    private val lockId: Long = 1000

    override suspend fun start() {
        val sessionRepository = injector.getInstance<SessionRepository>()
        val advisoryLock = injector.getInstance<AdvisoryLock>()

        try {
            Scheduler.schedule(vertx, this, "* */30 * * * ?", ZoneOffset.UTC) {
                advisoryLock.acquire(lockId) {
                    log.info("Starting ExpiredSessionsCleaner task")

                    try {
                        val deletedRecords = sessionRepository.deleteExpired().await()
                        log.info("Successfully deleted $deletedRecords expired sessions")
                    } catch (e: Exception) {
                        log.error("ExpiredSessionsCleaner task has failed", e)
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Failed to deploy ExpiredSessionsCleanerVerticle", e)
            throw e
        }
    }
}

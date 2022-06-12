package com.github.mkorman9.vertx.security

import com.github.mkorman9.vertx.tools.hibernate.AdvisoryLock
import com.github.mkorman9.vertx.utils.DeployVerticle
import com.google.inject.Injector
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.coroutines.CoroutineVerticle

@DeployVerticle
class ExpiredSessionsCleanerVerticle(
    injector: Injector
) : CoroutineVerticle() {
    companion object {
        private val log = LoggerFactory.getLogger(ExpiredSessionsCleanerVerticle::class.java)
    }

    private val lockId: Long = 1000
    private val taskDelayMs: Int = 30 * 60 * 1000  // 30 min

    private val sessionRepository = injector.getInstance<SessionRepository>()
    private val advisoryLock = injector.getInstance<AdvisoryLock>()

    override suspend fun start() {
        vertx.setPeriodic(taskDelayMs.toLong()) {
            advisoryLock.acquire(lockId) {
                log.info("Starting ExpiredSessionsCleaner task")

                sessionRepository.deleteExpired()
                    .onSuccess { deletedRecords -> log.info("Successfully deleted $deletedRecords expired sessions") }
                    .onFailure { failure -> log.error("ExpiredSessionsCleaner task has failed", failure) }
            }
        }

        log.info("ExpiredSessionsCleanerVerticle has been deployed successfully")
    }
}

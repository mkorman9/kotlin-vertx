package com.github.mkorman9.vertx.security

import com.github.mkorman9.vertx.AppContext
import com.github.mkorman9.vertx.BootstrapVerticle
import com.github.mkorman9.vertx.utils.AdvisoryLock
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.coroutines.CoroutineVerticle

class ExpiredSessionsCleanerVerticle : CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(ExpiredSessionsCleanerVerticle::class.java)

    private val lockId: Long = 1000
    private val taskDelayMs: Long = 30 * 60 * 1000  // 30 min

    private val context: AppContext = BootstrapVerticle.cachedContext
    private val advisoryLock = context.injector.getInstance(AdvisoryLock::class.java)
    private val sessionRepository = context.injector.getInstance(SessionRepository::class.java)

    override suspend fun start() {
        context.vertx.setPeriodic(taskDelayMs) {
            advisoryLock.acquire(lockId) { promise ->
                log.info("Starting ExpiredSessionsCleaner task")

                sessionRepository.deleteExpired()
                    .onSuccess { deletedRecords -> log.info("Successfully deleted $deletedRecords expired sessions") }
                    .onFailure { failure -> log.error("ExpiredSessionsCleaner task has failed", failure) }
                    .onComplete { promise.complete() }
            }
        }

        log.info("ExpiredSessionsCleanerVerticle has been deployed successfully")
    }
}

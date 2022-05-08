package com.github.mkorman9.vertx.security

import com.github.mkorman9.vertx.BootstrapVerticle
import com.github.mkorman9.vertx.utils.AdvisoryLock
import com.google.inject.Injector
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.coroutines.CoroutineVerticle

class ExpiredSessionsCleanerVerticle(
    passedInjector: Injector? = null
) : CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(ExpiredSessionsCleanerVerticle::class.java)

    private val lockId: Long = 1000
    private val taskDelayMs: Int = 30 * 60 * 1000  // 30 min

    private val injector: Injector = passedInjector ?: BootstrapVerticle.injector
    private val advisoryLock = injector.getInstance<AdvisoryLock>()
    private val sessionRepository = injector.getInstance<SessionRepository>()

    override suspend fun start() {
        vertx.setPeriodic(taskDelayMs.toLong()) {
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

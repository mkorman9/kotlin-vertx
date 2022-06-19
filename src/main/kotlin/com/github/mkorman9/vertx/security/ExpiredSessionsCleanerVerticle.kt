package com.github.mkorman9.vertx.security

import com.github.mkorman9.vertx.utils.ContextualVerticle
import com.github.mkorman9.vertx.utils.DeployVerticle
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.core.impl.logging.LoggerFactory

@DeployVerticle
class ExpiredSessionsCleanerVerticle : ContextualVerticle() {
    companion object {
        private val log = LoggerFactory.getLogger(ExpiredSessionsCleanerVerticle::class.java)
    }

    private val lockId: Long = 1000
    private val taskDelayMs: Int = 30 * 60 * 1000  // 30 min

    override suspend fun start() {
        val sessionRepository = injector.getInstance<SessionRepository>()

        try {
            vertx.setPeriodic(taskDelayMs.toLong()) {
                log.info("Starting ExpiredSessionsCleaner task")

                sessionRepository.deleteExpired(vertx)
                    .onSuccess { deletedRecords -> log.info("Successfully deleted $deletedRecords expired sessions") }
                    .onFailure { failure -> log.error("ExpiredSessionsCleaner task has failed", failure) }
            }
        } catch (e: Exception) {
            log.error("Failed to deploy ExpiredSessionsCleanerVerticle", e)
            throw e
        }
    }
}

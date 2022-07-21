package com.github.mkorman9.vertx.security

import com.github.mkorman9.vertx.common.Services
import com.github.mkorman9.vertx.utils.setCronPeriodic
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import java.time.ZoneOffset

class ExpiredSessionsCleanerVerticle(
    private val services: Services
) : CoroutineVerticle() {
    companion object {
        private val log = LoggerFactory.getLogger(ExpiredSessionsCleanerVerticle::class.java)
    }

    override suspend fun start() {
        val sessionRepository = services.sessionRepository

        try {
            vertx.setCronPeriodic("0 00 22 ? * *", ZoneOffset.UTC, this) {
                log.info("Starting ExpiredSessionsCleaner task")

                try {
                    val deletedRecords = sessionRepository.deleteExpired().await()
                    log.info("Successfully deleted $deletedRecords expired sessions")
                } catch (e: Exception) {
                    log.error("ExpiredSessionsCleaner task has failed", e)
                }
            }
        } catch (e: Exception) {
            log.error("Failed to deploy ExpiredSessionsCleanerVerticle", e)
            throw e
        }
    }
}

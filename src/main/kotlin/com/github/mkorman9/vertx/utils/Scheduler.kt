package com.github.mkorman9.vertx.utils

import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.quartz.CronExpression
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

object Scheduler {
    private val jobs = mutableMapOf<UUID, Long>()

    fun schedule(
        vertx: Vertx,
        scope: CoroutineScope,
        cronExpression: String,
        timezone: ZoneOffset,
        f: suspend () -> Unit
    ): UUID {
        val cron = CronExpression(cronExpression)
        cron.timeZone = TimeZone.getTimeZone(timezone)

        val jobId = UUID.randomUUID()

        doSchedule(vertx, scope, jobId, cron, timezone, f)

        return jobId
    }

    fun cancel(vertx: Vertx, jobId: UUID) {
        val timerId = jobs[jobId]
        if (timerId != null) {
            vertx.cancelTimer(timerId)
            jobs.remove(jobId)
        }
    }

    private fun doSchedule(
        vertx: Vertx,
        scope: CoroutineScope,
        jobId: UUID,
        cron: CronExpression,
        timezone: ZoneOffset,
        f: suspend () -> Unit
    ) {
        val nextRunTime = getNextRunTime(cron, timezone)
        val delay = ChronoUnit.MILLIS.between(LocalDateTime.now(timezone), nextRunTime)

        val timerId = vertx.setTimer(delay) {
            scope.launch {
                f()
            }

            doSchedule(vertx, scope, jobId, cron, timezone, f)
        }

        jobs[jobId] = timerId
    }

    private fun getNextRunTime(cron: CronExpression, timezone: ZoneOffset): LocalDateTime {
        return LocalDateTime.ofInstant(
            cron.getNextValidTimeAfter(
                Date.from(
                    LocalDateTime.now()
                        .plus(500, ChronoUnit.MILLIS)
                        .toInstant(timezone)
                )
            ).toInstant(),
            timezone
        )
    }
}

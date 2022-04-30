package com.github.mkorman9.vertx

import java.time.LocalDateTime

data class HealthcheckResponse(
    val status: String,
    val version: String,
    val startupTime: LocalDateTime
)

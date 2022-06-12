package com.github.mkorman9.vertx.utils.web

import java.time.LocalDateTime

data class HealthcheckResponse(
    val status: String,
    val version: String,
    val startupTime: LocalDateTime,
    val environment: String
)

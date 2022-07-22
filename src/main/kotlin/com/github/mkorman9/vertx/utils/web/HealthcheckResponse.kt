package com.github.mkorman9.vertx.utils.web

import java.time.Instant

data class HealthcheckResponse(
    val status: String,
    val version: String,
    val startupTime: Instant,
    val environment: String,
    val profile: String
)

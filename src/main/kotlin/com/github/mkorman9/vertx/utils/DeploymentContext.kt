package com.github.mkorman9.vertx.utils

import java.time.LocalDateTime

data class DeploymentContext(
    val version: String,
    val startupTime: LocalDateTime,
    val environment: String
)

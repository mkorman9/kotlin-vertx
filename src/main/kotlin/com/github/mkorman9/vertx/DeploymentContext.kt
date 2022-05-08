package com.github.mkorman9.vertx

import java.time.LocalDateTime

data class DeploymentContext(
    val version: String,
    val startupTime: LocalDateTime
)

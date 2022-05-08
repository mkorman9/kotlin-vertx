package com.github.mkorman9.vertx

import io.vertx.core.Vertx
import java.time.LocalDateTime

data class AppContext(
    val vertx: Vertx,
    val version: String,
    val startupTime: LocalDateTime
)

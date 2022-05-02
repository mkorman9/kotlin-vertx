package com.github.mkorman9.vertx

import com.google.inject.Injector
import io.vertx.core.Vertx
import java.time.LocalDateTime

data class AppContext(
    val vertx: Vertx,
    val injector: Injector,
    val version: String,
    val startupTime: LocalDateTime
)

package com.github.mkorman9.vertx.security

data class StartSessionPayload(
    val email: String,
    val password: String
)

package com.github.mkorman9.vertx.security

data class LoginPayload(
    val email: String,
    val password: String
)

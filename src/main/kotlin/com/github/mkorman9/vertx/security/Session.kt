package com.github.mkorman9.vertx.security

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime
import java.util.*

data class Session(
    var id: String? = null,
    val token: String,
    val roles: MutableSet<String>,
    val ip: String,
    val issuedAt: LocalDateTime,
    var duration: Int? = null,
    var expiresAt: LocalDateTime? = null,

    @JsonIgnore
    val account: Account
)

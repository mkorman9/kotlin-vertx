package com.github.mkorman9.vertx.security

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime

data class Account(
    var id: String? = null,
    var username: String,
    val roles: MutableSet<String>,
    var active: Boolean,
    var deleted: Boolean = false,
    var preferredLanguage: String,
    var bannedUntil: LocalDateTime? = null,
    var createdAt: LocalDateTime,

    @JsonIgnore
    var credentials: AccountCredentials
)

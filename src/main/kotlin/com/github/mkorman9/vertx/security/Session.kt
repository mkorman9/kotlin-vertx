package com.github.mkorman9.vertx.security

data class Session(
    var id: String = "",
    var accountId: String = "",
    var token: String = "",
    var roles: List<String> = listOf(),
    var ip: String = "",
    var issuedAt: Long = 0,
    var duration: Int? = null,
    var expiresAt: Long? = null,
)

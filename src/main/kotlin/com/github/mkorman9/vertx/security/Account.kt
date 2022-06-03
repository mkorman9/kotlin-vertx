package com.github.mkorman9.vertx.security

data class Account(
    var id: String = "",
    var username: String = "",
    var roles: List<String> = listOf(),
    var active: Boolean = false,
    var deleted: Boolean = false,
    var preferredLanguage: String = "",
    var bannedUntil: Long = 0,
    var createdAt: Long = 0,
    var credentials: AccountCredentials? = null
)

data class AccountCredentials(
    var email: String = "",
    var passwordBcrypt: String = "",
    var lastChangeAt: Long = 0,
    var lastChangeIp: String = "",
)

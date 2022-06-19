package com.github.mkorman9.vertx.security

import java.time.LocalDateTime

data class AccountCredentials(
    var email: String,
    var passwordBcrypt: String,
    var lastChangeAt: LocalDateTime,
    var lastChangeIp: String
)

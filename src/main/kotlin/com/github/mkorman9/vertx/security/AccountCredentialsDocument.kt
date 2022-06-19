package com.github.mkorman9.vertx.security

import java.time.LocalDateTime
import java.time.ZoneOffset

data class AccountCredentialsDocument(
    var email: String = "",
    var passwordBcrypt: String = "",
    var lastChangeAt: Long = 0,
    var lastChangeIp: String = ""
) {
    companion object {
        fun fromAccountCredentials(accountCredentials: AccountCredentials): AccountCredentialsDocument {
            return AccountCredentialsDocument(
                email = accountCredentials.email,
                passwordBcrypt = accountCredentials.passwordBcrypt,
                lastChangeAt = accountCredentials.lastChangeAt.toEpochSecond(ZoneOffset.UTC),
                lastChangeIp = accountCredentials.lastChangeIp
            )
        }
    }

    fun toAccountCredentials(): AccountCredentials {
        return AccountCredentials(
            email = email,
            passwordBcrypt = passwordBcrypt,
            lastChangeAt = LocalDateTime.ofEpochSecond(lastChangeAt, 0, ZoneOffset.UTC),
            lastChangeIp = lastChangeIp
        )
    }
}

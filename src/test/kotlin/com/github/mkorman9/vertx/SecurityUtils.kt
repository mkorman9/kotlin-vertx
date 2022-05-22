package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.security.Account
import com.github.mkorman9.vertx.security.Session
import java.time.LocalDateTime
import java.util.*

fun fakeSession(accountName: String, isBanned: Boolean = false): Session {
    val accountId = UUID.randomUUID()
    return Session(
        id = UUID.randomUUID().toString(),
        accountId = accountId,
        token = UUID.randomUUID().toString(),
        rolesString = "",
        ip = "127.0.0.1",
        issuedAt = LocalDateTime.now().minusMinutes(15),
        duration = 4 * 60 * 60 * 60,  // 4h
        expiresAt = LocalDateTime.now().plusHours(4),
        account = Account(
            id = accountId,
            username = accountName,
            rolesString = "",
            active = true,
            deleted = false,
            preferredLanguage = "en-US",
            bannedUntil = if (isBanned) LocalDateTime.MAX else LocalDateTime.MIN,
            createdAt = LocalDateTime.now().minusYears(1),
            credentials = null
        )
    )
}

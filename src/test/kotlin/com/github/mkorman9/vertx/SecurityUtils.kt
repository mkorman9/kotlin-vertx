package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.security.Account
import com.github.mkorman9.vertx.security.AccountCredentials
import com.github.mkorman9.vertx.security.Session
import java.time.LocalDateTime
import java.util.*

data class TestPassword(
    val plaintext: String,
    val bcryptEncoded: String
)

val defaultTestPassword = TestPassword(
    plaintext = "password",
    bcryptEncoded = "\$2a\$10\$CYoenxLkI.J6uzP3oH2Iceh9Zm1n4XO51ngkSwm3Rk7BfmXawkWW2"
)

fun fakeSession(
    accountName: String,
    isBanned: Boolean = false,
    email: String = "test.user@example.com",
    password: TestPassword = defaultTestPassword
): Session {
    val accountId = UUID.randomUUID()
    val session = Session(
        id = (Math.random() * 10_000).toLong(),
        token = UUID.randomUUID().toString(),
        ip = "127.0.0.1",
        issuedAt = LocalDateTime.now().minusMinutes(15),
        duration = 4 * 60 * 60 * 60,  // 4h
        expiresAt = LocalDateTime.now().plusHours(4),
        account = Account(
            id = accountId,
            username = accountName,
            active = true,
            deleted = false,
            preferredLanguage = "en-US",
            bannedUntil = if (isBanned) LocalDateTime.MAX else LocalDateTime.MIN,
            createdAt = LocalDateTime.now().minusYears(1),
            credentials = null
        )
    )

    session.account.credentials = AccountCredentials(
        email = email,
        passwordBcrypt = password.bcryptEncoded,
        lastChangeAt = session.account.createdAt,
        lastChangeIp = "127.0.0.1",
        account = session.account
    )

    return session
}

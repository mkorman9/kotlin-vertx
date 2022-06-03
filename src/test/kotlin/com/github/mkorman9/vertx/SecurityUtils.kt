package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.security.Account
import com.github.mkorman9.vertx.security.AccountCredentials
import com.github.mkorman9.vertx.security.MockCredentials
import com.github.mkorman9.vertx.security.Session
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

data class TestPassword(
    val plaintext: String,
    val bcryptEncoded: String
)

val defaultTestPassword = TestPassword(
    plaintext = "password",
    bcryptEncoded = "\$2a\$10\$CYoenxLkI.J6uzP3oH2Iceh9Zm1n4XO51ngkSwm3Rk7BfmXawkWW2"
)

fun fakeCredentials(
    accountName: String,
    isBanned: Boolean = false,
    email: String = "test.user@example.com",
    password: TestPassword = defaultTestPassword
): MockCredentials {
    val accountId = UUID.randomUUID().toString()

    val account = Account(
        id = accountId,
        username = accountName,
        roles = listOf(),
        active = true,
        deleted = false,
        preferredLanguage = "en-US",
        bannedUntil = (if (isBanned) LocalDateTime.MAX else LocalDateTime.MIN).toEpochSecond(ZoneOffset.UTC),
        createdAt = LocalDateTime.now().minusYears(1).toEpochSecond(ZoneOffset.UTC),
        credentials = AccountCredentials(
            email = email,
            passwordBcrypt = password.bcryptEncoded,
            lastChangeAt = LocalDateTime.now().minusYears(1).toEpochSecond(ZoneOffset.UTC),
            lastChangeIp = "127.0.0.1"
        )
    )

    val session = Session(
        id = UUID.randomUUID().toString(),
        accountId = accountId,
        token = UUID.randomUUID().toString(),
        roles = listOf(),
        ip = "127.0.0.1",
        issuedAt = LocalDateTime.now().minusMinutes(15).toEpochSecond(ZoneOffset.UTC),
        duration = 4 * 60 * 60 * 60,  // 4h
        expiresAt = LocalDateTime.now().plusHours(4).toEpochSecond(ZoneOffset.UTC),
    )

    return MockCredentials(session, account)
}

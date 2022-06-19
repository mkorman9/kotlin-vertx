package com.github.mkorman9.vertx.security

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

data class AccountDocument(
    val id: String = "",
    val username: String = "",
    val roles: List<String> = listOf(),
    val active: Boolean = true,
    val deleted: Boolean = false,
    val preferredLanguage: String = "",
    val bannedUntil: Long? = null,
    val createdAt: Long = 0,
    val credentials: AccountCredentialsDocument = AccountCredentialsDocument()
) {
    companion object {
        fun fromAccount(account: Account): AccountDocument {
            return AccountDocument(
                id = account.id.toString(),
                username = account.username,
                roles = account.roles.toList(),
                active = account.active,
                deleted = account.deleted,
                preferredLanguage = account.preferredLanguage,
                bannedUntil = account.bannedUntil?.toEpochSecond(ZoneOffset.UTC),
                createdAt = account.createdAt.toEpochSecond(ZoneOffset.UTC),
                credentials = AccountCredentialsDocument.fromAccountCredentials(account.credentials)
            )
        }
    }

    fun toAccount(): Account {
        return Account(
            id = UUID.fromString(id),
            username = username,
            roles = roles.toMutableSet(),
            active = active,
            deleted = deleted,
            preferredLanguage = preferredLanguage,
            bannedUntil =
                if (bannedUntil != null)
                    LocalDateTime.ofEpochSecond(bannedUntil, 0, ZoneOffset.UTC)
                else
                    null,
            createdAt = LocalDateTime.ofEpochSecond(createdAt, 0, ZoneOffset.UTC),
            credentials = credentials.toAccountCredentials()
        )
    }
}

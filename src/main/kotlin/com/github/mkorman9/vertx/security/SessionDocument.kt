package com.github.mkorman9.vertx.security

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

data class SessionDocument(
    val id: String = "",
    val accountId: String = "",
    val token: String = "",
    val roles: List<String> = listOf(),
    val ip: String = "",
    val issuedAt: Long = 0,
    val duration: Int? = null,
    val expiresAt: Long? = null,
) {
    companion object {
        fun fromSession(session: Session): SessionDocument {
            return SessionDocument(
                id = session.id.toString(),
                accountId = session.account.id.toString(),
                token = session.token,
                roles = session.roles.toList(),
                ip = session.ip,
                issuedAt = session.issuedAt.toEpochSecond(ZoneOffset.UTC),
                duration = session.duration,
                expiresAt = session.expiresAt?.toEpochSecond(ZoneOffset.UTC)
            )
        }
    }

    fun toSession(account: Account): Session {
        return Session(
            id = UUID.fromString(id),
            token = token,
            roles = roles.toMutableSet(),
            ip = ip,
            issuedAt = LocalDateTime.ofEpochSecond(issuedAt, 0, ZoneOffset.UTC),
            duration = duration,
            expiresAt =
                if (expiresAt != null)
                    LocalDateTime.ofEpochSecond(expiresAt, 0, ZoneOffset.UTC)
                else
                    null,
            account = account
        )
    }
}

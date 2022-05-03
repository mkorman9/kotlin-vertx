package com.github.mkorman9.vertx.security

import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.*

@Entity(name = "Session")
@Table(name = "sessions")
data class Session(
    @Id
    @Column(name = "id")
    var id: String,

    @Column(name = "account_id", columnDefinition = "uuid")
    var accountId: UUID,

    @Column(name = "token")
    var token: String,

    @Column(name = "roles")
    var rolesString: String,

    @Column(name = "ip")
    var ip: String,

    @Column(name = "issued_at")
    var issuedAt: LocalDateTime,

    @Column(name = "duration")
    var duration: Int? = null,

    @Column(name = "expires_at")
    var expiresAt: LocalDateTime? = null,

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false, insertable = false, updatable = false)
    val account: Account
) {
    var roles: Set<String>
        get() = rolesString.split(";").toHashSet()
        set(value) {
            rolesString = value.joinToString(";")
        }
}

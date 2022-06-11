package com.github.mkorman9.vertx.security

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.*

@Entity(name = "Session")
@Table(name = "sessions")
data class Session(
    @Id
    @Column(name = "id", columnDefinition = "text")
    var id: String,

    @Column(name = "account_id", columnDefinition = "uuid")
    var accountId: UUID,

    @Column(name = "token", columnDefinition = "text", unique = true)
    var token: String,

    @Column(name = "roles", columnDefinition = "text")
    @JsonIgnore
    var rolesString: String,

    @Column(name = "ip", columnDefinition = "text")
    var ip: String,

    @Column(name = "issued_at", columnDefinition = "timestamp")
    var issuedAt: LocalDateTime,

    @Column(name = "duration", columnDefinition = "integer")
    var duration: Int? = null,

    @Column(name = "expires_at", columnDefinition = "timestamp")
    var expiresAt: LocalDateTime? = null,

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false, insertable = false, updatable = false)
    @JsonIgnore
    val account: Account
) {
    var roles: Set<String>
        get() = rolesString.split(";").toHashSet()
        set(value) {
            rolesString = value.joinToString(";")
        }
}

package com.github.mkorman9.vertx.security

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.*

@Entity(name = "Session")
@Table(name = "sessions")
data class Session(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sessions_id_gen")
    @SequenceGenerator(name="sessions_id_gen", sequenceName = "sessions_id_seq")
    @Column(name = "id", columnDefinition = "bigint")
    @JsonIgnore
    val id: Long? = null,

    @Column(name = "account_id", columnDefinition = "uuid", nullable = false)
    var accountId: UUID,

    @Column(name = "token", columnDefinition = "text", unique = true, nullable = false)
    var token: String,

    @Column(name = "roles", columnDefinition = "text", nullable = false)
    @JsonIgnore
    var rolesString: String = "",

    @Column(name = "ip", columnDefinition = "text", nullable = false)
    var ip: String,

    @Column(name = "issued_at", columnDefinition = "timestamp", nullable = false)
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

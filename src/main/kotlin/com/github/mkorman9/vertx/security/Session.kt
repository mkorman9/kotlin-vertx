package com.github.mkorman9.vertx.security

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.mkorman9.vertx.tools.hibernate.types.StringSet
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import java.time.LocalDateTime
import javax.persistence.*

@Entity(name = "Session")
@Table(name = "sessions")
@TypeDefs(
    TypeDef(name = "string-set", typeClass = StringSet::class)
)
data class Session(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sessions_id_gen")
    @SequenceGenerator(name="sessions_id_gen", sequenceName = "sessions_id_seq")
    @Column(name = "id", columnDefinition = "bigint")
    @JsonIgnore
    val id: Long? = null,

    @Column(name = "token", columnDefinition = "text", nullable = false)
    var token: String,

    @Column(name = "roles", columnDefinition = "text[]", nullable = false)
    @Type(type = "string-set")
    val roles: MutableSet<String> = mutableSetOf(),

    @Column(name = "ip", columnDefinition = "text", nullable = false)
    var ip: String,

    @Column(name = "issued_at", columnDefinition = "timestamp", nullable = false)
    var issuedAt: LocalDateTime,

    @Column(name = "duration", columnDefinition = "integer")
    var duration: Int? = null,

    @Column(name = "expires_at", columnDefinition = "timestamp")
    var expiresAt: LocalDateTime? = null,

    @ManyToOne
    @JoinColumn(name = "account_id", columnDefinition = "uuid", nullable = false)
    @JsonIgnore
    val account: Account
) {
    companion object {
        const val TOKEN_UNIQUE_CONSTRAINT = "unique_sessions_token"
    }
}

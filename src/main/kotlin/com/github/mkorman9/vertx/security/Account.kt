package com.github.mkorman9.vertx.security

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.mkorman9.vertx.tools.hibernate.types.StringSet
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity(name = "Account")
@Table(name = "accounts",)
@TypeDefs(
    TypeDef(name = "string-set", typeClass = StringSet::class)
)
data class Account(
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    val id: UUID,

    @Column(name = "username", columnDefinition = "text", nullable = false)
    var username: String,

    @Column(name = "roles", columnDefinition = "text[]", nullable = false)
    @Type(type = "string-set")
    val roles: MutableSet<String> = mutableSetOf(),

    @Column(name = "active", columnDefinition = "boolean", nullable = false)
    var active: Boolean,

    @Column(name = "deleted", columnDefinition = "boolean", nullable = false)
    var deleted: Boolean,

    @Column(name = "preferred_language", columnDefinition = "text", nullable = false)
    var preferredLanguage: String,

    @Column(name = "banned_until", columnDefinition = "timestamp")
    var bannedUntil: LocalDateTime?,

    @Column(name = "created_at", columnDefinition = "timestamp", nullable = false)
    var createdAt: LocalDateTime,

    @OneToOne(
        fetch = FetchType.EAGER,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    @JoinColumn(name = "credentials_id", columnDefinition = "bigint")
    @JsonIgnore
    var credentials: AccountCredentials
) {
    companion object {
        const val USERNAME_UNIQUE_CONSTRAINT = "unique_accounts_username"
    }
}

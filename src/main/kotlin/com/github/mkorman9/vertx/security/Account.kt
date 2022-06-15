package com.github.mkorman9.vertx.security

import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.annotations.Type
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity(name = "Account")
@Table(name = "accounts")
data class Account(
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    val id: UUID,

    @Column(name = "username", columnDefinition = "text", nullable = false)
    var username: String,

    @Column(name = "roles", columnDefinition = "text[]", nullable = false)
    @Type(type = "com.github.mkorman9.vertx.tools.hibernate.types.StringList")
    var roles: List<String> = listOf(),

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
        mappedBy = "account",
        fetch = FetchType.EAGER,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    @JsonIgnore
    var credentials: AccountCredentials?
)

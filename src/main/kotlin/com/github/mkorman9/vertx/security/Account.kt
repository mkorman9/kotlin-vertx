package com.github.mkorman9.vertx.security

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.*


@Entity(name = "Account")
@Table(name = "accounts")
data class Account(
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID,

    @Column(name = "username")
    var username: String,

    @Column(name = "roles")
    @JsonIgnore
    var rolesString: String,

    @Column(name = "active")
    var active: Boolean,

    @Column(name = "deleted")
    var deleted: Boolean,

    @Column(name = "preferred_language")
    var preferredLanguage: String,

    @Column(name = "banned_until")
    var bannedUntil: LocalDateTime,

    @Column(name = "created_at")
    var createdAt: LocalDateTime,

    @OneToOne(
        mappedBy = "account",
        fetch = FetchType.EAGER,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    @JsonIgnore
    var credentials: AccountCredentials
) {
    var roles: Set<String>
        get() = rolesString.split(";").toHashSet()
        set(value) {
            rolesString = value.joinToString(";")
        }
}

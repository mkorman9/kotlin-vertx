package com.github.mkorman9.vertx.security

import java.time.LocalDateTime
import java.util.Objects
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity(name = "AccountCredentials")
@Table(name = "accounts_credentials")
data class AccountCredentials(
    @Id
    @Column(name = "account_id", columnDefinition = "uuid")
    var accountId: UUID,

    @Column(name = "email", columnDefinition = "text", unique = true, nullable = false)
    var email: String,

    @Column(name = "password_bcrypt", columnDefinition = "text", nullable = false)
    var passwordBcrypt: String,

    @Column(name = "last_change_at", columnDefinition = "timestamp")
    var lastChangeAt: LocalDateTime,

    @Column(name = "last_change_ip", columnDefinition = "text")
    var lastChangeIp: String,

    @OneToOne
    @JoinColumn(name = "account_id", columnDefinition = "uuid" , nullable = false)
    val account: Account
) {
    override fun hashCode(): Int {
        return Objects.hash(accountId, email, passwordBcrypt, lastChangeAt, lastChangeIp)
    }
}

package com.github.mkorman9.vertx.security

import java.time.LocalDateTime
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

    @Column(name = "email")
    var email: String,

    @Column(name = "password_bcrypt")
    var passwordBcrypt: String,

    @Column(name = "last_change_at")
    var lastChangeAt: LocalDateTime,

    @Column(name = "last_change_ip")
    var lastChangeIp: String,

    @OneToOne
    @JoinColumn(name = "account_id")
    val account: Account
)

package com.github.mkorman9.vertx.security

import java.time.LocalDateTime
import javax.persistence.*

@Entity(name = "AccountCredentials")
@Table(name = "accounts_credentials")
data class AccountCredentials(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accounts_credentials_id_gen")
    @SequenceGenerator(name="accounts_credentials_id_gen", sequenceName = "accounts_credentials_id_seq")
    @Column(name = "id", columnDefinition = "bigint")
    var id: Long? = null,

    @Column(name = "email", columnDefinition = "text", nullable = false)
    var email: String,

    @Column(name = "password_bcrypt", columnDefinition = "text", nullable = false)
    var passwordBcrypt: String,

    @Column(name = "last_change_at", columnDefinition = "timestamp")
    var lastChangeAt: LocalDateTime,

    @Column(name = "last_change_ip", columnDefinition = "text")
    var lastChangeIp: String
) {
    companion object {
        const val EMAIL_UNIQUE_CONSTRAINT = "unique_accounts_credentials_email"
    }
}

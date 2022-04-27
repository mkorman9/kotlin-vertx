package com.github.mkorman9.vertx.client

import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity(name = "Client")
@Table(name = "clients")
data class Client(
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    val id: UUID,

    @Column(name = "gender", columnDefinition = "CHAR(1)")
    val gender: String,

    @Column(name = "first_name")
    val firstName: String,

    @Column(name = "last_name")
    val lastName: String,

    @Column(name = "home_address")
    val address: String,

    @Column(name = "phone_number")
    val phoneNumber: String,

    @Column(name = "email")
    val email: String,

    @Column(name = "birth_date")
    val birthDate: LocalDateTime,

    @Column(name = "deleted")
    val deleted: Boolean
)

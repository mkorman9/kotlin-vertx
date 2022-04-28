package com.github.mkorman9.vertx.client

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.*

@Entity(name = "Client")
@Table(name = "clients")
data class Client(
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    val id: UUID,

    @Column(name = "gender", columnDefinition = "CHAR(1)")
    val gender: String = "-",

    @Column(name = "first_name")
    val firstName: String,

    @Column(name = "last_name")
    val lastName: String,

    @Column(name = "home_address")
    val address: String? = null,

    @Column(name = "phone_number")
    val phoneNumber: String? = null,

    @Column(name = "email")
    val email: String? = null,

    @Column(name = "birth_date")
    val birthDate: LocalDateTime? = null,

    @Column(name = "deleted")
    @JsonIgnore
    val deleted: Boolean = false,

    @OneToMany(mappedBy = "clientId", fetch = FetchType.EAGER)
    val creditCards: List<CreditCard> = listOf()
)

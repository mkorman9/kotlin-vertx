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
    var id: UUID,

    @Column(name = "gender", columnDefinition = "CHAR(1)")
    var gender: String = "-",

    @Column(name = "first_name")
    var firstName: String,

    @Column(name = "last_name")
    var lastName: String,

    @Column(name = "home_address")
    var address: String? = null,

    @Column(name = "phone_number")
    var phoneNumber: String? = null,

    @Column(name = "email")
    var email: String? = null,

    @Column(name = "birth_date")
    var birthDate: LocalDateTime? = null,

    @Column(name = "deleted")
    @JsonIgnore
    var deleted: Boolean = false,

    @OneToMany(mappedBy = "clientId", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    var creditCards: List<CreditCard> = listOf()
)

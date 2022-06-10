package com.github.mkorman9.vertx.client

import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.annotations.BatchSize
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.*

@Entity(name = "Client")
@Table(name = "clients")
data class Client(
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID,

    @Column(name = "gender", columnDefinition = "char(1)")
    var gender: String = "-",

    @Column(name = "first_name", columnDefinition = "text")
    var firstName: String,

    @Column(name = "last_name", columnDefinition = "text")
    var lastName: String,

    @Column(name = "home_address", columnDefinition = "text")
    var address: String? = null,

    @Column(name = "phone_number", columnDefinition = "text")
    var phoneNumber: String? = null,

    @Column(name = "email", columnDefinition = "text")
    var email: String? = null,

    @Column(name = "birth_date", columnDefinition = "timestamp")
    var birthDate: LocalDateTime? = null,

    @Column(name = "deleted", columnDefinition = "boolean")
    @JsonIgnore
    var deleted: Boolean = false,

    @OneToMany(
        mappedBy = "clientId",
        fetch = FetchType.EAGER,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    @BatchSize(size = 10)
    var creditCards: MutableList<CreditCard> = mutableListOf()
)

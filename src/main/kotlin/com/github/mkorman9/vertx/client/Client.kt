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
    val id: UUID,

    @Column(name = "gender", columnDefinition = "char(1)", nullable = false)
    var gender: String = "-",

    @Column(name = "first_name", columnDefinition = "text", nullable = false)
    var firstName: String,

    @Column(name = "last_name", columnDefinition = "text", nullable = false)
    var lastName: String,

    @Column(name = "home_address", columnDefinition = "text")
    var address: String? = null,

    @Column(name = "phone_number", columnDefinition = "text")
    var phoneNumber: String? = null,

    @Column(name = "email", columnDefinition = "text")
    var email: String? = null,

    @Column(name = "birth_date", columnDefinition = "timestamp")
    var birthDate: LocalDateTime? = null,

    @Column(name = "deleted", columnDefinition = "boolean", nullable = false)
    @JsonIgnore
    var deleted: Boolean = false,

    @OneToMany(
        fetch = FetchType.EAGER,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    @JoinColumn(name = "client_id", columnDefinition = "uuid")
    @BatchSize(size = 10)
    var creditCards: MutableList<CreditCard> = mutableListOf()
)

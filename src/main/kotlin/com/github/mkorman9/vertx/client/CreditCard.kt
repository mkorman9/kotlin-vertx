package com.github.mkorman9.vertx.client

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*
import javax.persistence.*

@Entity(name = "CreditCard")
@Table(name = "clients_credit_cards")
@IdClass(CreditCardId::class)
data class CreditCard(
    @Id
    @Column(name = "client_id", columnDefinition = "uuid")
    @JsonIgnore
    var clientId: UUID,

//    @ManyToOne
//    @JoinColumn(name = "client_id", nullable = false, insertable = false, updatable = false)
//    @JsonIgnore
//    val client: Client,

    @Id
    @Column(name = "number")
    var number: String
)

data class CreditCardId(
    val clientId: UUID? = null,
    val number: String? = null
) : java.io.Serializable

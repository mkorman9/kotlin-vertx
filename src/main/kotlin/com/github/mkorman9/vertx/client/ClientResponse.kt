package com.github.mkorman9.vertx.client

import java.time.LocalDateTime

data class ClientResponse(
    var id: String = "",
    var gender: String = "-",
    var firstName: String = "",
    var lastName: String = "",
    var address: String = "",
    var phoneNumber: String = "",
    var email: String = "",
    var birthDate: LocalDateTime? = null,
    var creditCards: List<CreditCardResponse> = listOf()
)

data class CreditCardResponse(
    var number: String = ""
)

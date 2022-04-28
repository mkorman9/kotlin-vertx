package com.github.mkorman9.vertx.client

import java.time.LocalDateTime

data class ClientAddPayload(
    val gender: String?,
    val firstName: String,
    val lastName: String,
    val address: String?,
    val phoneNumber: String?,
    val email: String?,
    val birthDate: LocalDateTime?,
    val creditCards: List<ClientAddCreditCardPayload>?
)

data class ClientAddCreditCardPayload(
    val number: String
)

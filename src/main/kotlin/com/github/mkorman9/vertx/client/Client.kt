package com.github.mkorman9.vertx.client

data class Client(
    var id: String = "",
    var gender: String = "-",
    var firstName: String = "",
    var lastName: String = "",
    var address: String = "",
    var phoneNumber: String = "",
    var email: String = "",
    var birthDate: Long? = null,
    var deleted: Boolean = false,
    var creditCards: List<CreditCard> = listOf()
)

data class CreditCard(
    var number: String = ""
)

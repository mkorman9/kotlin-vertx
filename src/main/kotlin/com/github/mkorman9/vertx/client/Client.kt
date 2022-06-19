package com.github.mkorman9.vertx.client

import java.time.LocalDateTime
import java.util.*

data class Client(
    var id: UUID? = null,
    var gender: String = "-",
    var firstName: String,
    var lastName: String,
    var address: String? = null,
    var phoneNumber: String? = null,
    var email: String? = null,
    var birthDate: LocalDateTime? = null,
    var deleted: Boolean = false,
    var creditCards: MutableList<String> = mutableListOf()
)

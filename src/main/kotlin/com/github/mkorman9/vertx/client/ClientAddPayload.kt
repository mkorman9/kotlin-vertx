package com.github.mkorman9.vertx.client

import java.time.LocalDateTime
import javax.validation.constraints.Email
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

data class ClientAddPayload(
    @field:Pattern(regexp = "^-|M|F$", message = "oneof")
    val gender: String?,

    @field:NotNull(message = "required")
    @field:Size(min = 1, max = 255, message = "size")
    val firstName: String,

    @field:NotNull(message = "required")
    @field:Size(min = 1, max = 255, message = "size")
    val lastName: String,

    @field:Size(min = 1, max = 1024, message = "size")
    val address: String?,

    @field:Size(min = 1, max = 64, message = "size")
    val phoneNumber: String?,

    @field:Email(message = "email")
    @field:Size(min = 1, max = 64, message = "size")
    val email: String?,

    val birthDate: LocalDateTime?,

    val creditCards: List<ClientAddCreditCardPayload>?
)

data class ClientAddCreditCardPayload(
    @field:NotNull(message = "required")
    @field:Pattern(regexp = "^\\d{4} \\d{4} \\d{4} \\d{4}\$", message = "ccnumber")
    val number: String
)

data class ClientAddResponse(
    val id: String
)

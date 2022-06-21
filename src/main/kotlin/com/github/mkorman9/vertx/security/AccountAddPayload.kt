package com.github.mkorman9.vertx.security

import javax.validation.constraints.Email
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

data class AccountAddPayload(
    @field:NotNull(message = "required")
    @field:Size(min = 3, max = 50, message = "size")
    val username: String,

    @field:NotNull(message = "required")
    @field:Size(max = 50, message = "size")
    @field:Email(message = "email")
    val email: String,

    @field:NotNull(message = "required")
    @field:Size(min = 3, message = "size")
    val password: String,

    @field:Pattern(regexp = "^en-US|pl-PL$", message = "oneof")
    val language: String = "en-US"
)

package com.github.mkorman9.vertx.utils

import com.fasterxml.jackson.annotation.JsonInclude

data class StatusDTO(
    val status: String,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    val message: String? = null,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    val causes: List<Cause>? = null
)

data class Cause(
    val field: String,
    val code: String
)

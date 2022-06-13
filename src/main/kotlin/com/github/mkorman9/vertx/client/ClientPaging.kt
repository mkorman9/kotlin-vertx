package com.github.mkorman9.vertx.client

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

data class ClientPage(
    val data: List<Client>,
    val page: Int,
    val totalPages: Int
)

data class ClientFilteringOptions(
    val gender: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val address: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val bornAfter: LocalDateTime? = null,
    val bornBefore: LocalDateTime? = null,
    val creditCard: String? = null
)

data class ClientPagingOptions(
    val pageNumber: Int,
    val pageSize: Int
)

data class ClientSortingOptions(
    val sortBy: String,
    val sortReverse: Boolean
)

data class ClientCursor(
    val data: List<Client>,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    val nextCursor: String?
)

data class ClientCursorOptions(
    val cursor: String?,
    val limit: Int
)

data class FindCursorClientParams(
    val filtering: ClientFilteringOptions,
    val cursorOptions: ClientCursorOptions
)

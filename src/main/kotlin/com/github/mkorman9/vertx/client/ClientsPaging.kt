package com.github.mkorman9.vertx.client

import java.time.LocalDateTime

data class ClientsPage(
    val data: List<Client>,
    val page: Int,
    val totalPages: Int
)

data class ClientsFilteringOptions(
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

data class ClientsPagingOptions(
    val pageNumber: Int,
    val pageSize: Int
)

data class ClientsSortingOptions(
    val sortBy: String,
    val sortReverse: Boolean
)

data class FindClientsParams(
    val filtering: ClientsFilteringOptions,
    val paging: ClientsPagingOptions,
    val sorting: ClientsSortingOptions
)

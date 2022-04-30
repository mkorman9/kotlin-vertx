package com.github.mkorman9.vertx.client

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

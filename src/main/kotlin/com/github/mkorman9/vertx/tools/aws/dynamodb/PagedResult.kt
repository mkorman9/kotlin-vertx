package com.github.mkorman9.vertx.tools.aws.dynamodb

import io.vertx.core.Future

typealias PagedResultFetcher<T> = () -> Future<PagedResult<T>>

data class PagedResult<T>(
    val items: List<T>,
    val count: Int,
    val scannedCount: Int,
    private val fetchNextPage: PagedResultFetcher<T>?
) {
    fun hasNextPage(): Boolean {
        return fetchNextPage != null
    }

    fun nextPage(): Future<PagedResult<T>> {
        if (fetchNextPage != null) {
            return fetchNextPage.invoke()
        } else {
            throw IllegalStateException("No more pages to fetch")
        }
    }
}

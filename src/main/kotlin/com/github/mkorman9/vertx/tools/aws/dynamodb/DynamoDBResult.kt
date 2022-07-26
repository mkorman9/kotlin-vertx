package com.github.mkorman9.vertx.tools.aws.dynamodb

import io.vertx.core.Future

typealias DynamoDBResultFetcher<T> = () -> Future<DynamoDBResult<T>>

data class DynamoDBResult<T>(
    val items: List<T>,
    val count: Int,
    val scannedCount: Int,
    private val fetchNextPage: DynamoDBResultFetcher<T>?
) {
    fun hasMorePages(): Boolean {
        return fetchNextPage != null
    }

    fun nextPage(): Future<DynamoDBResult<T>> {
        if (fetchNextPage != null) {
            return fetchNextPage.invoke()
        } else {
            throw IllegalStateException("No more pages to fetch")
        }
    }
}

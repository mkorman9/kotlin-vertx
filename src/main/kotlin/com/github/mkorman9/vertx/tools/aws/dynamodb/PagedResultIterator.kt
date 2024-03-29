package com.github.mkorman9.vertx.tools.aws.dynamodb

import io.vertx.core.Future
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PagedResultIterator<T> internal constructor(
    private var head: Future<PagedResult<T>>
): Iterator<Future<PagedResult<T>>>  {
    private var hasNextPage: Boolean = true

    override fun hasNext(): Boolean {
        return hasNextPage
    }

    override fun next(): Future<PagedResult<T>> {
        if (!hasNextPage) {
            throw IllegalStateException("Iteration overflow")
        }

        return head.onSuccess { result ->
            hasNextPage = result.hasNextPage()

            if (hasNextPage) {
                head = result.nextPage()
            }
        }
    }

    suspend fun forEachAsync(f: suspend (PagedResult<T>) -> Unit) {
        forEach { resultFuture ->
            val result = resultFuture.await()
            f(result)
        }
    }

    suspend fun items(): Flow<T> {
        return flow {
            forEachAsync { result ->
                result.items.forEach { item ->
                    emit(item)
                }
            }
        }
    }
}

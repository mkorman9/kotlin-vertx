package com.github.mkorman9.vertx.tools.aws.dynamodb

import io.vertx.core.Future

class PagedResultIterator<T> internal constructor(
    private var head: Future<PagedResult<T>>,
    private var hasNextPage: Boolean = true
): Iterator<Future<PagedResult<T>>>  {
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
}

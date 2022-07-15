package com.github.mkorman9.vertx.tools.aws

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler
import io.vertx.core.Promise

internal fun <REQUEST : AmazonWebServiceRequest, RESULT> createAsyncHandler(
    promise: Promise<RESULT>
): AsyncHandler<REQUEST, RESULT> {
    return object : AsyncHandler<REQUEST, RESULT> {
        override fun onError(exception: Exception?) {
            promise.fail(exception)
        }

        override fun onSuccess(request: REQUEST, result: RESULT) {
            promise.complete(result)
        }
    }
}

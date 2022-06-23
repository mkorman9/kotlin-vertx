package com.github.mkorman9.vertx.utils

import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun Vertx.setPeriodicCoroutine(
    delay: Long,
    scope: CoroutineScope,
    f: suspend (Long) -> Unit
) {
    setPeriodic(delay) { timerId ->
        scope.launch {
            f(timerId)
        }
    }
}

fun <T> EventBus.coroutineConsumer(
    address: String,
    scope: CoroutineScope,
    f: suspend (Message<T>) -> Unit
): MessageConsumer<T> {
    return consumer(address) { message ->
        scope.launch {
            f(message)
        }
    }
}

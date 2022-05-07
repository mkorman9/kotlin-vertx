package com.github.mkorman9.vertx.client

data class ClientEvent(
    val operation: ClientEventOperation,
    val clientId: String,
    val author: String
)

enum class ClientEventOperation {
    ADDED,
    UPDATED,
    DELETED
}

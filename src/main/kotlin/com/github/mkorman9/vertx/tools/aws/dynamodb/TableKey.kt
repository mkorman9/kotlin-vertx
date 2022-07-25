package com.github.mkorman9.vertx.tools.aws.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue

data class TableKey(
    val hashKey: Key,
    val sortKey: Key? = null
) {
    internal fun toMap(): Map<String, AttributeValue> {
        val map = mutableMapOf(
            hashKey.name to hashKey.value
        )

        if (sortKey != null) {
            map[sortKey.name] = sortKey.value
        }

        return map
    }
}

data class Key(
    val name: String,
    val value: AttributeValue
)

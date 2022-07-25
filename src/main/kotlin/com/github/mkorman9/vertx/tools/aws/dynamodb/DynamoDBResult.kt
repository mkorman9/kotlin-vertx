package com.github.mkorman9.vertx.tools.aws.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue

data class DynamoDBResult<T>(
    val items: List<T>,
    val count: Int,
    val scannedCount: Int,
    val lastKey: Map<String, AttributeValue>? = null
)

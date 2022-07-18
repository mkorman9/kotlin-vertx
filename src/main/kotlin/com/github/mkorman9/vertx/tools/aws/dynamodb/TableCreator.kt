package com.github.mkorman9.vertx.tools.aws.dynamodb

import com.amazonaws.services.dynamodbv2.model.BillingMode
import io.vertx.core.CompositeFuture
import io.vertx.core.Future

object TableCreator {
    fun createAll(client: DynamoDBClient, definitions: List<TableDefinition>): CompositeFuture {
        val futures = mutableListOf<Future<*>>()

        definitions.forEach { tableDefinition ->
            val f = client.createTable(
                tableClass = tableDefinition.tableClass,
                billingMode = tableDefinition.billingMode,
                readCapacity = tableDefinition.readCapacity,
                writeCapacity = tableDefinition.writeCapacity
            )

            futures.add(f)
        }

        return CompositeFuture.all(futures)
    }
}

data class TableDefinition(
    val tableClass: Class<*>,
    val billingMode: BillingMode = BillingMode.PAY_PER_REQUEST,
    val readCapacity: Long = 3000,
    val writeCapacity: Long = 1000,
)

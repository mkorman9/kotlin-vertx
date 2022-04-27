package com.github.mkorman9.vertx

import io.vertx.core.Future
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import java.util.stream.Collectors
import java.util.stream.StreamSupport

class ClientRepository(
    private val sqlClient: SqlClient
) {
    private val ClientMapper = { row: Row ->
        Client(
            firstName = row.getString("first_name"),
            lastName = row.getString("last_name")
        )
    }

    fun findClients(): Future<List<Client>> {
        return sqlClient
            .query("SELECT first_name, last_name from clients LIMIT 10")
            .execute()
            .map { rs ->
                StreamSupport.stream(rs.spliterator(), false)
                    .map { ClientMapper(it) }
                    .collect(Collectors.toList())
            }
    }
}

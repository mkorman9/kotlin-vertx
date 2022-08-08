package com.github.mkorman9.vertx.tools.postgres

import com.github.mkorman9.vertx.utils.core.Config
import com.github.mkorman9.vertx.utils.core.get
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.jdbc.impl.JDBCClientImpl
import io.vertx.ext.sql.SQLConnection
import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import java.sql.Connection

object LiquibaseExecutor {
    private const val CHANGELOG_PATH = "classpath:/liquibase/changelog.xml"

    fun migrateSchema(vertx: Vertx, config: Config): Future<Void> {
        val uri = config.get<String>("DB_URI") ?: throw RuntimeException("DB_URI is missing from config")
        val user = config.get<String>("DB_USER") ?: throw RuntimeException("DB_USER is missing from config")
        val password = config.get<String>("DB_PASSWORD")
            ?: throw RuntimeException("DB_PASSWORD is missing from config")

        val client = JDBCClient.createShared(
            vertx,
            JsonObject()
                .put("url", uri)
                .put("user", user)
                .put("password", password)
                .put("driver_class", "org.postgresql.Driver")
                .put("acquire_retry_attempts", 3)
                .put("acquire_retry_delay", 1000)
        ) as JDBCClientImpl

        return client.connection
            .map<Connection>(SQLConnection::unwrap)
            .map { connection ->
                val database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(JdbcConnection(connection))
                val resourceAccessor = ClassLoaderResourceAccessor()

                Liquibase(CHANGELOG_PATH, resourceAccessor, database)
            }
            .compose { liquibase ->
                vertx.executeBlocking { call ->
                    liquibase.update(null as Contexts?)
                    client.close()

                    call.complete()
                }
            }
    }
}

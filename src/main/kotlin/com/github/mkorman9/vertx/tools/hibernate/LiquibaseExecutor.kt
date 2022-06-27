package com.github.mkorman9.vertx.tools.hibernate

import com.github.mkorman9.vertx.utils.Config
import com.github.mkorman9.vertx.utils.get
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

class LiquibaseExecutor {
    companion object {
        fun migrateSchema(vertx: Vertx, config: Config) {
            val uri = config.get<String>("db.uri") ?: throw RuntimeException("db.uri is missing from config")
            val user = config.get<String>("db.user") ?: throw RuntimeException("db.user is missing from config")
            val password = config.get<String>("db.password")
                ?: throw RuntimeException("db.password is missing from config")

            val client = JDBCClient.createShared(
                vertx,
                JsonObject()
                    .put("url", uri)
                    .put("user", user)
                    .put("password", password)
                    .put("driver_class", "org.postgresql.Driver")
            ) as JDBCClientImpl

            val connection = client.connection
                .map<Connection>(SQLConnection::unwrap)
                .toCompletionStage()
                .toCompletableFuture()
                .join()
            val database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(JdbcConnection(connection))

            val changelog = "classpath:/liquibase/changelog.xml"
            val resourceAccessor = ClassLoaderResourceAccessor()
            val liquibase = Liquibase(changelog, resourceAccessor, database)

            liquibase.update(null as Contexts?)

            client.close()
        }
    }
}

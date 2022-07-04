package com.github.mkorman9.vertx.tools.postgres

import com.github.mkorman9.vertx.utils.Config
import com.github.mkorman9.vertx.utils.get
import io.vertx.core.Vertx
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient

object PostgresInitializer {
    fun initialize(vertx: Vertx, config: Config): SqlClient {
        val host = config.get<String>("db.host") ?: throw RuntimeException("db.host is missing from config")
        val database = config.get<String>("db.database") ?: throw RuntimeException("db.database is missing from config")
        val user = config.get<String>("db.user") ?: throw RuntimeException("db.user is missing from config")
        val password = config.get<String>("db.password")
            ?: throw RuntimeException("db.password is missing from config")
        val port = config.get<Int>("db.port") ?: 5432
        val tls = config.get<Boolean>("db.tls") ?: false

        val poolSize = config.get<Int>("db.pool.size") ?: 8
        val connectTimeout = config.get<Int>("db.pool.timeouts.connect") ?: 30
        val idleTimeout = config.get<Int>("db.pool.timeouts.idle") ?: 0
        val cleanerPeriod = config.get<Int>("db.pool.cleaner") ?: 1000

        val connectOptions = PgConnectOptions()
            .setHost(host)
            .setPort(port)
            .setDatabase(database)
            .setUser(user)
            .setPassword(password)
            .setSsl(tls)

        val poolOptions = PoolOptions()
            .setMaxSize(poolSize)
            .setConnectionTimeout(connectTimeout)
            .setIdleTimeout(idleTimeout)
            .setPoolCleanerPeriod(cleanerPeriod)

        return PgPool.client(vertx, connectOptions, poolOptions)
    }
}

package com.github.mkorman9.vertx.tools.postgres

import com.github.mkorman9.vertx.utils.core.Config
import com.github.mkorman9.vertx.utils.core.get
import io.vertx.core.Vertx
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient

object PostgresInitializer {
    fun initialize(vertx: Vertx, config: Config): SqlClient {
        val host = config.get<String>("DB_HOST") ?: throw RuntimeException("DB_HOST is missing from config")
        val database = config.get<String>("DB_DATABASE") ?: throw RuntimeException("DB_DATABASE is missing from config")
        val user = config.get<String>("DB_USER") ?: throw RuntimeException("DB_USER is missing from config")
        val password = config.get<String>("DB_PASSWORD")
            ?: throw RuntimeException("DB_PASSWORD is missing from config")
        val port = config.get<Int>("DB_PORT") ?: 5432
        val tls = config.get<Boolean>("DB_TLS") ?: false

        val poolSize = config.get<Int>("DB_POOL_SIZE") ?: 8
        val connectTimeout = config.get<Int>("DB_POOL_TIMEOUTS_CONNECT") ?: 30
        val idleTimeout = config.get<Int>("DB_POOL_TIMEOUTS_IDLE") ?: 0
        val cleanerPeriod = config.get<Int>("DB_POOL_CLEANER_PERIOD") ?: 1000

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

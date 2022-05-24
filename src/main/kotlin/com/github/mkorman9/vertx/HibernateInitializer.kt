package com.github.mkorman9.vertx

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import javax.persistence.Persistence

class HibernateInitializer {
    private lateinit var sessionFactory: SessionFactory

    fun start(vertx: Vertx, config: JsonObject): Future<SessionFactory> {
        val uri = config.getJsonObject("db")?.getString("uri")
            ?: throw RuntimeException("db.uri is missing from config")
        val user = config.getJsonObject("db")?.getString("user")
            ?: throw RuntimeException("db.user is missing from config")
        val password = config.getJsonObject("db")?.getString("password")
            ?: throw RuntimeException("db.password is missing from config")

        val poolConfig = config.getJsonObject("db")?.getJsonObject("pool")
        val poolSize = poolConfig?.getInteger("size") ?: 5
        val connectTimeout = poolConfig?.getInteger("connectTimeout") ?: 30_000
        val idleTimeout = poolConfig?.getInteger("idleTimeout") ?: 0
        val cleanerPeriod = poolConfig?.getInteger("cleanerPeriod") ?: 1000

        val sqlConfig = config.getJsonObject("db")?.getJsonObject("sql")
        val showSql = sqlConfig?.getBoolean("show") ?: false
        val formatSql = sqlConfig?.getBoolean("format") ?: false
        val highlightSql = sqlConfig?.getBoolean("highlight") ?: false

        val props = mapOf(
            "hibernate.connection.url" to uri,
            "hibernate.connection.username" to user,
            "hibernate.connection.password" to password,

            "hibernate.connection.pool_size" to poolSize,
            "hibernate.vertx.pool.connect_timeout" to connectTimeout,
            "hibernate.vertx.pool.idle_timeout" to idleTimeout,
            "hibernate.vertx.pool.cleaner_period" to cleanerPeriod,

            "hibernate.show_sql" to showSql,
            "hibernate.format_sql" to formatSql,
            "hibernate.highlight_sql" to highlightSql
        )

        return vertx
            .executeBlocking<SessionFactory?> { call ->
                val sessionFactory = Persistence
                    .createEntityManagerFactory("default", props)
                    .unwrap(SessionFactory::class.java)

                call.complete(sessionFactory)
            }
            .map {
                sessionFactory = it
                it
            }
    }

    fun stop(vertx: Vertx): Future<Void> {
        return vertx.executeBlocking { promise ->
            sessionFactory.close()
            promise.complete()
        }
    }
}

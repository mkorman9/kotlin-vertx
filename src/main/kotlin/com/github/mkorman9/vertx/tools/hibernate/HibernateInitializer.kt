package com.github.mkorman9.vertx.tools.hibernate

import com.github.mkorman9.vertx.utils.Config
import com.github.mkorman9.vertx.utils.get
import io.vertx.core.Future
import io.vertx.core.Vertx
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import javax.persistence.Persistence

object HibernateInitializer {
    fun initialize(vertx: Vertx, config: Config): Future<SessionFactory> {
        val uri = config.get<String>("DB_URI") ?: throw RuntimeException("DB_URI is missing from config")
        val user = config.get<String>("DB_USER") ?: throw RuntimeException("DB_USER is missing from config")
        val password = config.get<String>("DB_PASSWORD")
            ?: throw RuntimeException("DB_PASSWORD is missing from config")

        val poolSize = config.get<Int>("DB_POOL_SIZE") ?: 8
        val connectTimeout = config.get<Int>("DB_POOL_TIMEOUTS_CONNECT") ?: 30_000
        val idleTimeout = config.get<Int>("DB_POOL_TIMEOUTS_IDLE") ?: 0
        val cleanerPeriod = config.get<Int>("DB_POOL_CLEANER_PERIOD") ?: 1000

        val showSql = config.get<Boolean>("DB_SQL_SHOW") ?: false
        val formatSql = config.get<Boolean>("DB_SQL_FORMAT") ?: false
        val highlightSql = config.get<Boolean>("DB_SQL_HIGHLIGHT") ?: false

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

        // Hibernate needs to be initialized inside the context of Vert.x thread pool
        return vertx.executeBlocking { call ->
            val sessionFactory = Persistence
                .createEntityManagerFactory("default", props)
                .unwrap(SessionFactory::class.java)

            call.complete(sessionFactory)
        }
    }
}

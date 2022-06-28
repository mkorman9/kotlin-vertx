package com.github.mkorman9.vertx.tools.hibernate

import com.github.mkorman9.vertx.utils.Config
import com.github.mkorman9.vertx.utils.get
import io.vertx.core.Future
import io.vertx.core.Vertx
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import javax.persistence.Persistence

object HibernateInitializer {
    fun initialize(vertx: Vertx, config: Config): Future<SessionFactory> {
        val uri = config.get<String>("db.uri") ?: throw RuntimeException("db.uri is missing from config")
        val user = config.get<String>("db.user") ?: throw RuntimeException("db.user is missing from config")
        val password = config.get<String>("db.password")
            ?: throw RuntimeException("db.password is missing from config")

        val poolSize = config.get<Int>("db.pool.size")  ?: 8
        val connectTimeout = config.get<Int>("db.pool.timeouts.connect") ?: 30_000
        val idleTimeout = config.get<Int>("db.pool.timeouts.idle") ?: 0
        val cleanerPeriod = config.get<Int>("db.pool.cleaner") ?: 1000

        val showSql = config.get<Boolean>("db.sql.show") ?: false
        val formatSql = config.get<Boolean>("db.sql.format") ?: false
        val highlightSql = config.get<Boolean>("db.sql.highlight") ?: false

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
            try {
                val sessionFactory = Persistence
                    .createEntityManagerFactory("default", props)
                    .unwrap(SessionFactory::class.java)

                call.complete(sessionFactory)
            } catch (e: Exception) {
                call.fail(e)
            }
        }
    }
}

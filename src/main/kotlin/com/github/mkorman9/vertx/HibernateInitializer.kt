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

        val poolSize = config.getJsonObject("db")?.getJsonObject("pool")?.getInteger("size") ?: 5

        val props = mapOf(
            "javax.persistence.jdbc.url" to uri,
            "javax.persistence.jdbc.user" to user,
            "javax.persistence.jdbc.password" to password,

            "hibernate.connection.pool_size" to poolSize
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

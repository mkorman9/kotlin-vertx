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

        val props = mapOf(
            "javax.persistence.jdbc.url" to uri,
            "javax.persistence.jdbc.user" to user,
            "javax.persistence.jdbc.password" to password
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

    fun stop(): Future<Void> {
        return Future.succeededFuture()
    }
}

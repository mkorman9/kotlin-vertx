package com.github.mkorman9.vertx

import io.vertx.core.Vertx
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory

data class AppContext(
    val vertx: Vertx,
    val config: Config,
    val sessionFactory: SessionFactory
)

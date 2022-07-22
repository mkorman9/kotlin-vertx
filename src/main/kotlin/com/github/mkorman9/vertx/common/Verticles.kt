package com.github.mkorman9.vertx.common

import com.github.mkorman9.vertx.GrpcServerVerticle
import com.github.mkorman9.vertx.HttpServerVerticle
import com.github.mkorman9.vertx.client.ClientEventsVerticle
import com.github.mkorman9.vertx.security.ExpiredSessionsCleanerVerticle
import com.github.mkorman9.vertx.utils.VerticleDefinition

fun createVerticleDefinitions(services: Services): List<VerticleDefinition> {
    return listOf(
        VerticleDefinition(
            name = "HttpServerVerticle",
            create = { HttpServerVerticle(services) },
            instances = Integer.max(Runtime.getRuntime().availableProcessors(), 4)
        ),
        VerticleDefinition(
            name = "GrpcServerVerticle",
            create = { GrpcServerVerticle(services) }
        ),
        VerticleDefinition(
            name = "ClientEventsVerticle",
            create = { ClientEventsVerticle(services) }
        ),
        VerticleDefinition(
            name = "ExpiredSessionsCleanerVerticle",
            create = { ExpiredSessionsCleanerVerticle(services) }
        )
    )
}

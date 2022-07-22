package com.github.mkorman9.vertx.common

import com.github.mkorman9.vertx.GrpcServerVerticle
import com.github.mkorman9.vertx.HttpServerVerticle
import com.github.mkorman9.vertx.client.ClientEventsVerticle
import com.github.mkorman9.vertx.security.ExpiredSessionsCleanerVerticle
import com.github.mkorman9.vertx.utils.VerticleDefinition

fun getVerticlesToDeploy(services: Services): List<VerticleDefinition> {
    return listOf(
        VerticleDefinition(
            create = { HttpServerVerticle(services) },
            instances = Integer.max(Runtime.getRuntime().availableProcessors(), 4)
        ),
        VerticleDefinition(
            create = { GrpcServerVerticle(services) }
        ),
        VerticleDefinition(
            create = { ClientEventsVerticle(services) }
        ),
        VerticleDefinition(
            create = { ExpiredSessionsCleanerVerticle(services) }
        )
    )
}

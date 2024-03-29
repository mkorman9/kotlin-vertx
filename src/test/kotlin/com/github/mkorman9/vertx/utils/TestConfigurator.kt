package com.github.mkorman9.vertx.utils

import com.github.mkorman9.vertx.HttpServerVerticle
import com.github.mkorman9.vertx.common.Services
import com.github.mkorman9.vertx.utils.core.Config
import com.github.mkorman9.vertx.utils.core.DeploymentInfo
import com.github.mkorman9.vertx.utils.core.JsonCodec
import io.mockk.mockk
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx

object TestConfigurator {
    init {
        JsonCodec.configure()
    }

    fun createServices(): Services {
        return Services(
            sessionFactory = mockk(relaxed = true),
            sqsClient = mockk(relaxed = true),
            sessionRepository = mockk(relaxed = true),
            clientEventsPublisher = mockk(relaxed = true),
            clientRepository = mockk(relaxed = true),
            accountRepository = mockk(relaxed = true),
            authorizationMiddleware = mockk(relaxed = true)
        )
    }

    fun deployHttpServer(vertx: Vertx, services: Services): Future<Int> {
        DeploymentInfo.initialize(Config())

        val verticle = HttpServerVerticle(services)

        return vertx
            .deployVerticle(
                verticle,
                DeploymentOptions()
                    .setConfig(
                        Config()
                            .put("SERVER_PORT", 0)
                    )
            )
            .map {
                verticle.getPort()
            }
    }
}

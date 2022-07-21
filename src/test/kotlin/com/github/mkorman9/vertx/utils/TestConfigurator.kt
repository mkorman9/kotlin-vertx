package com.github.mkorman9.vertx.utils

import com.github.mkorman9.vertx.common.Services
import io.mockk.mockk

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
}

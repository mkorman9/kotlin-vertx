package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.createTestAppContext
import com.github.mkorman9.vertx.createTestHttpServer
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class, MockKExtension::class)
class ClientRouterTest {
    @MockK
    private lateinit var clientRepository: ClientRepository

    @BeforeEach
    fun setupRouter(vertx: Vertx, testContext: VertxTestContext) {
        val context = createTestAppContext(vertx)
        val clientRouter = createClientRouter(context, clientRepository)

        createTestHttpServer(vertx, testContext, clientRouter)
    }

    @Test
    fun stubTest() {

    }
}

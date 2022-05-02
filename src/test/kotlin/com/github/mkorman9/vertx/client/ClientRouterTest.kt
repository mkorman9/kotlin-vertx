package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.createTestAppContext
import com.github.mkorman9.vertx.createTestHttpServer
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import kotlin.test.assertEquals

@ExtendWith(VertxExtension::class, MockKExtension::class)
class ClientRouterTest {
    @MockK
    private lateinit var clientRepository: ClientRepository

    @BeforeEach
    fun setupRouter(vertx: Vertx, testContext: VertxTestContext) {
        val context = createTestAppContext(vertx)
        val clientRouter = ClientRouter(context, clientRepository).router

        createTestHttpServer(vertx, testContext, clientRouter)
    }

    @Test
    fun testFindById(vertx: Vertx, testContext: VertxTestContext) {
        val id = "c9720047-b769-4345-9c60-a94339f46e08"
        val client = Client(
            id = UUID.randomUUID(),
            firstName = "Test",
            lastName = "User"
        )

        every { clientRepository.findById(id) } returns Future.succeededFuture(client)

        val httpClient = vertx.createHttpClient()
        httpClient.request(HttpMethod.GET, 8080, "127.0.0.1", "/${id}")
            .compose { it.send() }
            .onSuccess { result ->
                assertEquals(200, result.statusCode())

                result.body()
                    .onSuccess { body ->
                        val receivedClient = DatabindCodec.mapper().readValue(body.bytes, Client::class.java)
                        assertEquals(client, receivedClient)

                        testContext.completeNow()
                    }
                    .onFailure { testContext.failNow(it) }
            }
            .onFailure { testContext.failNow(it) }
    }

    @Test
    fun testFindByIdMissingClient(vertx: Vertx, testContext: VertxTestContext) {
        val id = "c9720047-b769-4345-9c60-a94339f46e08"

        every { clientRepository.findById(id) } returns Future.succeededFuture(null)

        val httpClient = vertx.createHttpClient()
        httpClient.request(HttpMethod.GET, 8080, "127.0.0.1", "/${id}")
            .compose { it.send() }
            .onSuccess { result ->
                assertEquals(404, result.statusCode())

                testContext.completeNow()
            }
            .onFailure { testContext.failNow(it) }
    }
}

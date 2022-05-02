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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

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
    @DisplayName("should return client when queried by id")
    fun testFindById(vertx: Vertx, testContext: VertxTestContext) {
        val id = UUID.randomUUID().toString()
        val client = Client(
            id = UUID.fromString(id),
            firstName = "Test",
            lastName = "User"
        )

        every { clientRepository.findById(id) } returns Future.succeededFuture(client)

        val httpClient = vertx.createHttpClient()
        httpClient.request(HttpMethod.GET, 8080, "127.0.0.1", "/${id}")
            .compose { it.send() }
            .onSuccess { result ->
                assertThat(result.statusCode()).isEqualTo(200)

                result.body()
                    .onSuccess { body ->
                        val receivedClient = DatabindCodec.mapper().readValue(body.bytes, Client::class.java)
                        assertThat(receivedClient).isEqualTo(client)

                        testContext.completeNow()
                    }
                    .onFailure { testContext.failNow(it) }
            }
            .onFailure { testContext.failNow(it) }
    }

    @Test
    @DisplayName("should return 404 when queried by id of non-existing client")
    fun testFindByIdMissingClient(vertx: Vertx, testContext: VertxTestContext) {
        val id = UUID.randomUUID().toString()

        every { clientRepository.findById(id) } returns Future.succeededFuture(null)

        val httpClient = vertx.createHttpClient()
        httpClient.request(HttpMethod.GET, 8080, "127.0.0.1", "/${id}")
            .compose { it.send() }
            .onSuccess { result ->
                assertThat(result.statusCode()).isEqualTo(404)

                testContext.completeNow()
            }
            .onFailure { testContext.failNow(it) }
    }
}

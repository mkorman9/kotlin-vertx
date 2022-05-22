package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.HttpServerVerticle
import com.github.mkorman9.vertx.asyncTest
import com.github.mkorman9.vertx.createTestInjector
import com.github.mkorman9.vertx.security.AuthorizationMiddleware
import com.github.mkorman9.vertx.security.AuthorizationMiddlewareMock
import com.github.mkorman9.vertx.security.MockSessionProvider
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.await
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(VertxExtension::class, MockKExtension::class)
class ClientApiTest {
    @SpyK
    private var clientRepository: ClientRepository = mockk()
    @MockK
    private lateinit var sessionProvider: MockSessionProvider

    @BeforeEach
    fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        val injector = createTestInjector(vertx, object : KotlinModule() {
            override fun configure() {
                bind<ClientApi>()
                bind<ClientRepository>().toInstance(clientRepository)
                bind<AuthorizationMiddleware>().toInstance(AuthorizationMiddlewareMock(sessionProvider))
            }
        })

        vertx.deployVerticle(HttpServerVerticle(injector))
            .onComplete { testContext.completeNow() }
    }

    @Test
    @DisplayName("should return paged clients with default settings when queried without parameters")
    fun testDefaultPaging(vertx: Vertx, testContext: VertxTestContext) = asyncTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val page = ClientsPage(
            page = 1,
            totalPages = 1,
            data = listOf(
                Client(
                    id = UUID.randomUUID(),
                    firstName = "Test",
                    lastName = "User"
                )
            )
        )

        every { clientRepository.findPaged(any(), any(), any()) } returns Future.succeededFuture(page)

        // when
        val result =
            httpClient.request(HttpMethod.GET, 8080, "127.0.0.1", "/api/v1/client")
                .await()
                .send()
                .await()
        val returnedPage = Json.decodeValue(result.body().await(), ClientsPage::class.java)

        // then
        assertThat(result.statusCode()).isEqualTo(200)
        assertThat(returnedPage).isEqualTo(page)

        verify { clientRepository.findPaged(
            filtering = ClientsFilteringOptions(),
            paging = ClientsPagingOptions(pageNumber = 1, pageSize = 10),
            sorting = ClientsSortingOptions(sortBy = "id", sortReverse = false)
        ) }
    }

    @Test
    @DisplayName("should return paged clients with specified settings when queried with parameters")
    fun testSpecificPaging(vertx: Vertx, testContext: VertxTestContext) = asyncTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val page = ClientsPage(
            page = 2,
            totalPages = 2,
            data = listOf(
                Client(
                    id = UUID.randomUUID(),
                    firstName = "Test",
                    lastName = "User"
                )
            )
        )

        every { clientRepository.findPaged(any(), any(), any()) } returns Future.succeededFuture(page)

        // when
        val result =
            httpClient.request(
                HttpMethod.GET,
                8080,
                "127.0.0.1",
                "/api/v1/client?filter[lastName]=User&page=2&pageSize=20&sortBy=firstName&sortReverse"
            )
                .await()
                .send()
                .await()
        val returnedPage = Json.decodeValue(result.body().await(), ClientsPage::class.java)

        // then
        assertThat(result.statusCode()).isEqualTo(200)
        assertThat(returnedPage).isEqualTo(page)

        verify { clientRepository.findPaged(
            filtering = ClientsFilteringOptions(lastName = "User"),
            paging = ClientsPagingOptions(pageNumber = 2, pageSize = 20),
            sorting = ClientsSortingOptions(sortBy = "firstName", sortReverse = true)
        ) }
    }

    @Test
    @DisplayName("should return paged clients with default settings when queried with invalid parameters")
    fun testInvalidPaging(vertx: Vertx, testContext: VertxTestContext) = asyncTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val page = ClientsPage(
            page = 1,
            totalPages = 1,
            data = listOf(
                Client(
                    id = UUID.randomUUID(),
                    firstName = "Test",
                    lastName = "User"
                )
            )
        )

        every { clientRepository.findPaged(any(), any(), any()) } returns Future.succeededFuture(page)

        // when
        val result =
            httpClient.request(
                HttpMethod.GET,
                8080,
                "127.0.0.1",
                "/api/v1/client?filter[invalid]=xxx&page=-2&pageSize=200&sortBy=unknown"
            )
                .await()
                .send()
                .await()
        val returnedPage = Json.decodeValue(result.body().await(), ClientsPage::class.java)

        // then
        assertThat(result.statusCode()).isEqualTo(200)
        assertThat(returnedPage).isEqualTo(page)

        verify { clientRepository.findPaged(
            filtering = ClientsFilteringOptions(),
            paging = ClientsPagingOptions(pageNumber = 1, pageSize = 100),
            sorting = ClientsSortingOptions(sortBy = "id", sortReverse = false)
        ) }
    }

    @Test
    @DisplayName("should return client when queried by id")
    fun testFindById(vertx: Vertx, testContext: VertxTestContext) = asyncTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val id = UUID.randomUUID().toString()
        val client = Client(
            id = UUID.fromString(id),
            firstName = "Test",
            lastName = "User"
        )

        every { clientRepository.findById(id) } returns Future.succeededFuture(client)

        // when
        val result =
            httpClient.request(HttpMethod.GET, 8080, "127.0.0.1", "/api/v1/client/${id}")
                .await()
                .send()
                .await()
        val returnedClient = Json.decodeValue(result.body().await(), Client::class.java)

        // then
        assertThat(result.statusCode()).isEqualTo(200)
        assertThat(returnedClient).isEqualTo(client)
    }

    @Test
    @DisplayName("should return 404 when queried by id of non-existing client")
    fun testFindByIdMissingClient(vertx: Vertx, testContext: VertxTestContext) = asyncTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val id = UUID.randomUUID().toString()

        every { clientRepository.findById(id) } returns Future.succeededFuture(null)

        // when
        val result =
            httpClient.request(HttpMethod.GET, 8080, "127.0.0.1", "/api/v1/client/${id}")
                .await()
                .send()
                .await()

        // then
        assertThat(result.statusCode()).isEqualTo(404)
    }
}

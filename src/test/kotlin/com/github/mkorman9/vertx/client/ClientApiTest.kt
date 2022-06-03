package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.HttpServerVerticle
import com.github.mkorman9.vertx.asyncTest
import com.github.mkorman9.vertx.createTestInjector
import com.github.mkorman9.vertx.fakeCredentials
import com.github.mkorman9.vertx.security.*
import com.github.mkorman9.vertx.utils.Cause
import com.github.mkorman9.vertx.utils.StatusDTO
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
import io.vertx.core.json.JsonObject
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
    private lateinit var credentialsProvider: MockCredentialsProvider
    @SpyK
    private var clientEventsPublisher: ClientEventsPublisher = mockk()

    @BeforeEach
    fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        val injector = createTestInjector(vertx, object : KotlinModule() {
            override fun configure() {
                bind<ClientApi>()
                bind<ClientRepository>().toInstance(clientRepository)
                bind<AuthorizationMiddleware>().toInstance(AuthorizationMiddlewareMock(credentialsProvider))
                bind<ClientEventsPublisher>().toInstance(clientEventsPublisher)
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
            data = listOf(
                Client(
                    id = UUID.randomUUID().toString(),
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
            data = listOf(
                Client(
                    id = UUID.randomUUID().toString(),
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
            data = listOf(
                Client(
                    id = UUID.randomUUID().toString(),
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
            id = id,
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

    @Test
    @DisplayName("should add new client when called with valid payload")
    fun testAddClient(vertx: Vertx, testContext: VertxTestContext) = asyncTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val payload = ClientAddPayload(
            firstName = "Test",
            lastName = "User"
        )
        val addedClient = Client(
            id = UUID.randomUUID().toString(),
            firstName = payload.firstName,
            lastName = payload.lastName
        )
        val credentials = fakeCredentials("test.account")

        every { clientRepository.add(payload) } returns Future.succeededFuture(addedClient)
        every { credentialsProvider.getCredentials() } returns credentials
        every { clientEventsPublisher.publish(any()) } returns Unit

        // when
        val result =
            httpClient.request(HttpMethod.POST, 8080, "127.0.0.1", "/api/v1/client")
                .await()
                .send(Json.encodeToBuffer(payload))
                .await()
        val clientAddResponse = Json.decodeValue(result.body().await(), ClientAddResponse::class.java)

        // then
        assertThat(result.statusCode()).isEqualTo(200)
        assertThat(clientAddResponse.id).isEqualTo(addedClient.id.toString())

        verify { clientEventsPublisher.publish(
            ClientEvent(
                operation = ClientEventOperation.ADDED,
                clientId = addedClient.id.toString(),
                author = credentials.account.id
            )
        ) }
    }

    @Test
    @DisplayName("should return 400 when trying to add client without firstName field")
    fun testAddNoFirstName(vertx: Vertx, testContext: VertxTestContext) = asyncTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val payload = JsonObject()
            .put("lastName", "User")
        val credentials = fakeCredentials("test.account")

        every { credentialsProvider.getCredentials() } returns credentials

        // when
        val result =
            httpClient.request(HttpMethod.POST, 8080, "127.0.0.1", "/api/v1/client")
                .await()
                .send(payload.encode())
                .await()
        val statusResponse = Json.decodeValue(result.body().await(), StatusDTO::class.java)

        // then
        assertThat(result.statusCode()).isEqualTo(400)
        assertThat(statusResponse.causes).isEqualTo(listOf(
            Cause("firstName", "required")
        ))
    }

    @Test
    @DisplayName("should return 400 when trying to add client without lastName field")
    fun testAddNoLastName(vertx: Vertx, testContext: VertxTestContext) = asyncTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val payload = JsonObject()
            .put("firstName", "Test")
        val credentials = fakeCredentials("test.account")

        every { credentialsProvider.getCredentials() } returns credentials

        // when
        val result =
            httpClient.request(HttpMethod.POST, 8080, "127.0.0.1", "/api/v1/client")
                .await()
                .send(payload.encode())
                .await()
        val statusResponse = Json.decodeValue(result.body().await(), StatusDTO::class.java)

        // then
        assertThat(result.statusCode()).isEqualTo(400)
        assertThat(statusResponse.causes).isEqualTo(listOf(
            Cause("lastName", "required")
        ))
    }

    @Test
    @DisplayName("should return 400 when trying to add client with invalid email")
    fun testAddInvalidFields(vertx: Vertx, testContext: VertxTestContext) = asyncTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val payload = ClientAddPayload(
            firstName = "Test",
            lastName = "User",
            email = "xxx"
        )
        val credentials = fakeCredentials("test.account")

        every { credentialsProvider.getCredentials() } returns credentials

        // when
        val result =
            httpClient.request(HttpMethod.POST, 8080, "127.0.0.1", "/api/v1/client")
                .await()
                .send(Json.encodeToBuffer(payload))
                .await()
        val statusResponse = Json.decodeValue(result.body().await(), StatusDTO::class.java)

        // then
        assertThat(result.statusCode()).isEqualTo(400)
        assertThat(statusResponse.causes).isEqualTo(listOf(
            Cause("email", "email")
        ))
    }

    @Test
    @DisplayName("should update existing client when called with valid payload")
    fun testUpdateClient(vertx: Vertx, testContext: VertxTestContext) = asyncTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val payload = ClientUpdatePayload(
            email = "test.user@example.com"
        )
        val client = Client(
            id = UUID.randomUUID().toString(),
            firstName = "Test",
            lastName = "User",
            email = "test.user@example.com"
        )
        val credentials = fakeCredentials("test.account")

        every { clientRepository.update(client.id.toString(), payload) } returns Future.succeededFuture(client)
        every { credentialsProvider.getCredentials() } returns credentials
        every { clientEventsPublisher.publish(any()) } returns Unit

        // when
        val result =
            httpClient.request(HttpMethod.PUT, 8080, "127.0.0.1", "/api/v1/client/${client.id}")
                .await()
                .send(Json.encodeToBuffer(payload))
                .await()
        val statusResponse = Json.decodeValue(result.body().await(), StatusDTO::class.java)

        // then
        assertThat(result.statusCode()).isEqualTo(200)
        assertThat(statusResponse.status).isEqualTo("ok")

        verify { clientEventsPublisher.publish(
            ClientEvent(
                operation = ClientEventOperation.UPDATED,
                clientId = client.id.toString(),
                author = credentials.account.id.toString()
            )
        ) }
    }

    @Test
    @DisplayName("should return 404 when trying to update non-existing client")
    fun testUpdateNonExistingClient(vertx: Vertx, testContext: VertxTestContext) = asyncTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val payload = ClientUpdatePayload(
            email = "test.user@example.com"
        )
        val clientId = UUID.randomUUID()
        val credentials = fakeCredentials("test.account")

        every { clientRepository.update(clientId.toString(), payload) } returns Future.succeededFuture(null)
        every { credentialsProvider.getCredentials() } returns credentials

        // when
        val result =
            httpClient.request(HttpMethod.PUT, 8080, "127.0.0.1", "/api/v1/client/$clientId")
                .await()
                .send(Json.encodeToBuffer(payload))
                .await()

        // then
        assertThat(result.statusCode()).isEqualTo(404)
    }

    @Test
    @DisplayName("should delete existing client when called with valid id")
    fun testDeleteClient(vertx: Vertx, testContext: VertxTestContext) = asyncTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val clientId = UUID.randomUUID()
        val credentials = fakeCredentials("test.account")

        every { clientRepository.delete(clientId.toString()) } returns Future.succeededFuture(true)
        every { credentialsProvider.getCredentials() } returns credentials
        every { clientEventsPublisher.publish(any()) } returns Unit

        // when
        val result =
            httpClient.request(HttpMethod.DELETE, 8080, "127.0.0.1", "/api/v1/client/$clientId")
                .await()
                .send()
                .await()
        val statusResponse = Json.decodeValue(result.body().await(), StatusDTO::class.java)

        // then
        assertThat(result.statusCode()).isEqualTo(200)
        assertThat(statusResponse.status).isEqualTo("ok")

        verify { clientEventsPublisher.publish(
            ClientEvent(
                operation = ClientEventOperation.DELETED,
                clientId = clientId.toString(),
                author = credentials.account.id.toString()
            )
        ) }
    }

    @Test
    @DisplayName("should return 404 when trying to delete non-existing client")
    fun testDeleteNonExistingClient(vertx: Vertx, testContext: VertxTestContext) = asyncTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val clientId = UUID.randomUUID()
        val credentials = fakeCredentials("test.account")

        every { clientRepository.delete(clientId.toString()) } returns Future.succeededFuture(false)
        every { credentialsProvider.getCredentials() } returns credentials

        // when
        val result =
            httpClient.request(HttpMethod.DELETE, 8080, "127.0.0.1", "/api/v1/client/$clientId")
                .await()
                .send()
                .await()

        // then
        assertThat(result.statusCode()).isEqualTo(404)
    }
}

package com.github.mkorman9.vertx.session

import com.github.mkorman9.vertx.defaultTestPassword
import com.github.mkorman9.vertx.fakeSession
import com.github.mkorman9.vertx.security.*
import com.github.mkorman9.vertx.utils.TestConfigurator
import com.github.mkorman9.vertx.utils.coroutineTest
import com.github.mkorman9.vertx.utils.web.StatusDTO
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
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

@ExtendWith(VertxExtension::class, MockKExtension::class)
class SessionApiTest {
    private var port: Int = 0

    @MockK
    private lateinit var sessionRepository: SessionRepository
    @MockK
    private lateinit var accountRepository: AccountRepository
    @MockK
    private lateinit var sessionProvider: MockSessionProvider

    @BeforeEach
    fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        val services = TestConfigurator
            .createServices()
            .copy(
                sessionRepository = sessionRepository,
                accountRepository = accountRepository,
                authorizationMiddleware = AuthorizationMiddlewareMock(sessionProvider)
            )

        TestConfigurator.deployHttpServer(vertx, services)
            .onSuccess {
                port = it
                testContext.completeNow()
            }
    }

    @Test
    @DisplayName("should authorize user by credentials and start new session")
    fun testStartNewSession(vertx: Vertx, testContext: VertxTestContext) = coroutineTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val payload = StartSessionPayload(
            email = "test.user@example.com",
            password = defaultTestPassword.plaintext
        )
        val session = fakeSession("test.user", email = payload.email, password = defaultTestPassword)

        every { accountRepository.findByCredentialsEmail(payload.email) } returns Future.succeededFuture(session.account)
        every { sessionRepository.add(any()) } returns Future.succeededFuture(session)

        // when
        val result =
            httpClient.request(HttpMethod.POST, port, "127.0.0.1", "/api/v1/session")
                .await()
                .send(Json.encodeToBuffer(payload))
                .await()

        // then
        assertThat(result.statusCode()).isEqualTo(200)
    }

    @Test
    @DisplayName("should deny authorization with invalid email")
    fun testStartNewSessionInvalidEmail(vertx: Vertx, testContext: VertxTestContext) = coroutineTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val payload = StartSessionPayload(
            email = "test.user@example.com",
            password = defaultTestPassword.plaintext
        )

        every { accountRepository.findByCredentialsEmail(payload.email) } returns Future.succeededFuture(null)

        // when
        val result =
            httpClient.request(HttpMethod.POST, port, "127.0.0.1", "/api/v1/session")
                .await()
                .send(Json.encodeToBuffer(payload))
                .await()

        // then
        assertThat(result.statusCode()).isEqualTo(401)
    }

    @Test
    @DisplayName("should deny authorization with invalid password")
    fun testStartNewSessionInvalidPassword(vertx: Vertx, testContext: VertxTestContext) = coroutineTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val payload = StartSessionPayload(
            email = "test.user@example.com",
            password = "invalid_password"
        )
        val session = fakeSession("test.user", email = payload.email, password = defaultTestPassword)

        every { accountRepository.findByCredentialsEmail(payload.email) } returns Future.succeededFuture(session.account)

        // when
        val result =
            httpClient.request(HttpMethod.POST, port, "127.0.0.1", "/api/v1/session")
                .await()
                .send(Json.encodeToBuffer(payload))
                .await()

        // then
        assertThat(result.statusCode()).isEqualTo(401)
    }

    @Test
    @DisplayName("should refresh active session")
    fun testRefreshSession(vertx: Vertx, testContext: VertxTestContext) = coroutineTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val session = fakeSession("test.user")

        every { sessionProvider.getSession() } returns session
        every { sessionRepository.refresh(session) } returns Future.succeededFuture(session)

        // when
        val result =
            httpClient.request(HttpMethod.PUT, port, "127.0.0.1", "/api/v1/session")
                .await()
                .send()
                .await()

        // then
        assertThat(result.statusCode()).isEqualTo(200)
    }

    @Test
    @DisplayName("should revoke active session")
    fun testRevokeSession(vertx: Vertx, testContext: VertxTestContext) = coroutineTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val session = fakeSession("test.user")

        every { sessionProvider.getSession() } returns session
        every { sessionRepository.delete(session) } returns Future.succeededFuture(true)

        // when
        val result =
            httpClient.request(HttpMethod.DELETE, port, "127.0.0.1", "/api/v1/session")
                .await()
                .send()
                .await()
        val statusResponse = Json.decodeValue(result.body().await(), StatusDTO::class.java)

        // then
        assertThat(result.statusCode()).isEqualTo(200)
        assertThat(statusResponse.status).isEqualTo("ok")
    }
}

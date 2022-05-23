package com.github.mkorman9.vertx.session

import com.github.mkorman9.vertx.HttpServerVerticle
import com.github.mkorman9.vertx.asyncTest
import com.github.mkorman9.vertx.client.ClientsPage
import com.github.mkorman9.vertx.createTestInjector
import com.github.mkorman9.vertx.fakeSession
import com.github.mkorman9.vertx.security.*
import com.github.mkorman9.vertx.utils.StatusDTO
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
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
    @SpyK
    private val sessionRepository: SessionRepository = mockk()
    @MockK
    private lateinit var sessionProvider: MockSessionProvider

    @BeforeEach
    fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        val injector = createTestInjector(vertx, object : KotlinModule() {
            override fun configure() {
                bind<SessionApi>()
                bind<SessionRepository>().toInstance(sessionRepository)
                bind<AuthorizationMiddleware>().toInstance(AuthorizationMiddlewareMock(sessionProvider))
            }
        })

        vertx.deployVerticle(HttpServerVerticle(injector))
            .onComplete { testContext.completeNow() }
    }

    @Test
    @DisplayName("should refresh active session")
    fun testRefreshSession(vertx: Vertx, testContext: VertxTestContext) = asyncTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val session = fakeSession("test.user")

        every { sessionProvider.getSession() } returns session
        every { sessionRepository.refresh(session) } returns Future.succeededFuture(session)

        // when
        val result =
            httpClient.request(HttpMethod.PUT, 8080, "127.0.0.1", "/api/v1/session")
                .await()
                .send()
                .await()

        // then
        assertThat(result.statusCode()).isEqualTo(200)
    }

    @Test
    @DisplayName("should revoke active session")
    fun testRevokeSession(vertx: Vertx, testContext: VertxTestContext) = asyncTest(vertx, testContext) {
        // given
        val httpClient = vertx.createHttpClient()
        val session = fakeSession("test.user")

        every { sessionProvider.getSession() } returns session
        every { sessionRepository.delete(session) } returns Future.succeededFuture(true)

        // when
        val result =
            httpClient.request(HttpMethod.DELETE, 8080, "127.0.0.1", "/api/v1/session")
                .await()
                .send()
                .await()
        val statusResponse = Json.decodeValue(result.body().await(), StatusDTO::class.java)

        // then
        assertThat(result.statusCode()).isEqualTo(200)
        assertThat(statusResponse.status).isEqualTo("ok")
    }
}

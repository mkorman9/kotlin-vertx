package com.github.mkorman9.vertx

import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.mockk
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.http.httpServerOptionsOf
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator
import java.time.LocalDateTime
import javax.validation.Validation
import javax.validation.Validator

fun createTestAppContext(vertx: Vertx): AppContext {
    configureJsonCodec()

    return AppContext(
        vertx = vertx,
        config = mockk(),
        sessionFactory = mockk(),
        validator = createBeanValidator(),
        version = "test",
        startupTime = LocalDateTime.now()
    )
}

fun createTestHttpServer(vertx: Vertx, testContext: VertxTestContext, handler: Handler<HttpServerRequest>) {
    vertx.createHttpServer(
        httpServerOptionsOf(
            host = "127.0.0.1",
            port = 8080
        )
    )
        .requestHandler { handler.handle(it) }
        .listen(testContext.succeedingThenComplete())
}

private fun createBeanValidator(): Validator {
    return Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(ParameterMessageInterpolator())
        .buildValidatorFactory()
        .validator
}
private fun configureJsonCodec() {
    val objectMapper = DatabindCodec.mapper()
    objectMapper.registerModule(KotlinModule.Builder().build())
    objectMapper.registerModule(JavaTimeModule())

    objectMapper.dateFormat = StdDateFormat()
}


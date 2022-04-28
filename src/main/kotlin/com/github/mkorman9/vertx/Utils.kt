package com.github.mkorman9.vertx

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.RoutingContext
import java.util.function.Consumer

fun HttpServerResponse.endWithJson(obj: Any) {
    putHeader("Content-Type", "application/json")
        .end(Json.encode(obj))
}

inline fun <reified T> RoutingContext.handleJsonBody(func: Consumer<T>) {
    request().bodyHandler { body ->
        val payload = try {
            DatabindCodec.mapper().readValue(body.bytes, T::class.java)
        } catch (e: JsonMappingException) {
            val field = e.path[0].fieldName
            val code = if (e.cause is MissingKotlinParameterException) "required" else "format"

            response().setStatusCode(400).endWithJson(
                StatusDTO(
                    status = "error",
                    message = "error while mapping request body",
                    causes = listOf(
                        Cause(field, code)
                    )
                )
            )

            return@bodyHandler
        } catch (e: Exception) {
            response().setStatusCode(400).endWithJson(
                StatusDTO(
                    status = "error",
                    message = "malformed request body"
                )
            )

            return@bodyHandler
        }

        func.accept(payload)
    }
}

package com.github.mkorman9.vertx

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.RoutingContext
import java.util.function.Consumer
import javax.validation.Validator

fun HttpServerResponse.endWithJson(obj: Any) {
    putHeader("Content-Type", "application/json")
        .end(Json.encode(obj))
}

inline fun <reified T> RoutingContext.handleJsonBody(validator: Validator? = null, func: Consumer<T>) {
    request().bodyHandler { body ->
        val payload = try {
            DatabindCodec.mapper().readValue(body.bytes, T::class.java)
        } catch (e: JsonMappingException) {
            val field = buildJsonExceptionPath(e.path)
            val code =
                if (e is MissingKotlinParameterException || e.cause is MissingKotlinParameterException) "required"
                else "format"

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

        if (validator != null) {
            val violations = validator.validate(payload)
            if (violations.isNotEmpty()) {
                response().setStatusCode(400).endWithJson(
                    StatusDTO(
                        status = "error",
                        message = "validation error",
                        causes = violations.map {
                            Cause(it.propertyPath.toString(), it.message)
                        }
                    )
                )

                return@bodyHandler
            }
        }

        func.accept(payload)
    }
}

fun buildJsonExceptionPath(path: List<JsonMappingException.Reference>): String {
    val parts = mutableListOf<String>()

    path.forEach { p ->
        if (p.fieldName != null) {
            parts.add(p.fieldName)
        } else {
            if (parts.isEmpty()) {
                parts.add("")
            }
            
            parts[parts.size - 1] += "[${p.index}]"
        }
    }

    return parts.joinToString(".")
}

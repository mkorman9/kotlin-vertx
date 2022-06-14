package com.github.mkorman9.vertx.utils.web

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.DecodeException
import io.vertx.core.json.Json
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun Route.asyncHandler(scope: CoroutineScope, func: suspend (RoutingContext) -> Unit): Route = handler { ctx ->
    scope.launch(ctx.vertx().dispatcher()) {
        try {
            func(ctx)
        } catch (t: Throwable) {
            ctx.fail(500, t)
        }
    }
}

fun HttpServerRequest.getClientIp(): String {
    return getHeader("X-Forwarded-For") ?: remoteAddress().host()
}

fun HttpServerResponse.endWithJson(obj: Any) {
    putHeader("Content-Type", "application/json")
        .end(Json.encode(obj))
}

suspend inline fun <reified T> RoutingContext.handleJsonBody(crossinline func: suspend (T) -> Unit) {
    val buffer = body().buffer()
    if (buffer == null) {
        response().setStatusCode(400).endWithJson(
            StatusDTO(
                status = "error",
                message = "empty request body"
            )
        )

        return
    }

    val payload = try {
        Json.decodeValue(buffer, T::class.java)
    } catch (e: DecodeException) {
        if (e.cause !is JsonMappingException) {
            response().setStatusCode(400).endWithJson(
                StatusDTO(
                    status = "error",
                    message = "malformed request body"
                )
            )

            return
        }

        response().setStatusCode(400).endWithJson(
            StatusDTO(
                status = "error",
                message = "error while mapping request body",
                causes = listOf(parseJsonExceptionCause(e))
            )
        )

        return
    }

    val violations = CommonValidator.validate(payload)
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

        return
    }

    func(payload)
}

fun parseJsonExceptionCause(e: DecodeException): Cause {
    val field = buildJsonExceptionPath((e.cause as JsonMappingException).path)
    val code = when (e.cause) {
        is MissingKotlinParameterException -> "required"
        is InvalidFormatException -> "format"
        else -> "invalid"
    }

    return Cause(field, code)
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

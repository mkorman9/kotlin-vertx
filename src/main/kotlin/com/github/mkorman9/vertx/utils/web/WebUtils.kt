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

fun Route.coroutineHandler(scope: CoroutineScope, f: suspend (RoutingContext) -> Unit): Route = handler { ctx ->
    scope.launch(ctx.vertx().dispatcher()) {
        try {
            f(ctx)
        } catch (t: Throwable) {
            ctx.fail(500, t)
        }
    }
}

fun HttpServerRequest.getClientIp(): String {
    /*
        GCP Load Balancer sets up the X-Forwarded-For header in the following format:
        X-Forwarded-For: <supplied-value>,<client-ip>,<load-balancer-ip>
        We need to extract the middle part, or the first one in case if there was no supplied value
    */
    val address = getHeader("X-Forwarded-For") ?: remoteAddress().host()
    val parts = address.split(',')

    return if (parts.size > 2) {
        parts[parts.size - 2]
    } else {
        parts[0]
    }
}

fun HttpServerRequest.isClientTLS(): Boolean {
    return isSSL || getHeader("X-Forwarded-Proto")?.lowercase() == "https"
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
        if (e.cause is JsonMappingException) {
            response().setStatusCode(400).endWithJson(
                StatusDTO(
                    status = "error",
                    message = "error while mapping request body",
                    causes = listOf(parseJsonExceptionCause(e.cause as JsonMappingException))
                )
            )
        } else {
            response().setStatusCode(400).endWithJson(
                StatusDTO(
                    status = "error",
                    message = "malformed request body"
                )
            )
        }

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

fun parseJsonExceptionCause(e: JsonMappingException): Cause {
    val field = buildJsonExceptionPath(e.path)
    val code = when (e) {
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

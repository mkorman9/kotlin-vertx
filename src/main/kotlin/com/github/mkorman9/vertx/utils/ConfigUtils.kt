package com.github.mkorman9.vertx.utils

import io.vertx.core.json.JsonObject
import io.vertx.core.json.pointer.JsonPointer

typealias Config = JsonObject

inline fun <reified T> Config.get(path: String): T? {
    val pointer = "/${path.replace(".", "/")}"
    val value = JsonPointer.from(pointer).queryJson(this)

    return if (value is T?) {
        value
    } else {
        null
    }
}

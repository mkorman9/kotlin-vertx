package com.github.mkorman9.vertx.utils

import io.vertx.core.json.JsonObject
import io.vertx.core.json.pointer.JsonPointer

typealias Config = JsonObject

inline fun <reified T> Config.get(path: String): T? {
    val value = JsonPointer.from("/$path").queryJson(this)

    return if (value is T?) {
        value
    } else {
        null
    }
}

package com.github.mkorman9.vertx.tools.aws

import com.amazonaws.services.sqs.model.Message
import io.vertx.core.json.JsonObject

fun Message.getContent(): String {
    return JsonObject(body).getString("Message")
}

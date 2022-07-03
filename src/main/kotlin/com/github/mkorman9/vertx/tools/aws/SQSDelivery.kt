package com.github.mkorman9.vertx.tools.aws

import com.amazonaws.services.sqs.model.Message
import io.vertx.core.json.JsonObject

class SQSDelivery internal constructor(
    rawMessage: Message
) {
    val content: String
    val message: Message

    init {
        content = JsonObject(rawMessage.body).getString("Message")
        message = rawMessage
    }
}

package com.github.mkorman9.vertx.tools.aws

import io.vertx.core.json.JsonObject
import javax.jms.Message
import javax.jms.TextMessage

class SQSDelivery internal constructor(
    jmsMessage: Message
) {
    val content: String
    val message: Message

    init {
        content = if (jmsMessage is TextMessage) {
            JsonObject(jmsMessage.text).getString("Message")
        } else {
            ""
        }

        message = jmsMessage
    }
}

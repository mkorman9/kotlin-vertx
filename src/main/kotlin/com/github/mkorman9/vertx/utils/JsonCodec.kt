package com.github.mkorman9.vertx.utils

import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.vertx.core.json.jackson.DatabindCodec

object JsonCodec {
    fun configure() {
        val objectMapper = DatabindCodec.mapper()
        objectMapper.registerModule(KotlinModule.Builder().build())
        objectMapper.registerModule(JavaTimeModule())
        objectMapper.dateFormat = StdDateFormat()
    }
}

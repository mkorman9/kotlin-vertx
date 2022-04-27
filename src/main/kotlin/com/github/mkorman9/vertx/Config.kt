package com.github.mkorman9.vertx

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.vertx.core.buffer.Buffer

data class Config(
    val server: ServerConfig?,
    val db: DatabaseConfig
)

data class ServerConfig(
    val host: String?,
    val port: Int?
)

data class DatabaseConfig(
    val uri: String,
    val user: String,
    val password: String
)

fun parseConfig(buffer: Buffer): Config {
    val mapper = ObjectMapper(YAMLFactory())
    mapper.registerModule(KotlinModule.Builder().build())

    return mapper.readValue(buffer.bytes, Config::class.java)
}

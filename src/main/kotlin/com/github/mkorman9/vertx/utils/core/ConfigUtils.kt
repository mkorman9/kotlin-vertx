package com.github.mkorman9.vertx.utils.core

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.json.pointer.JsonPointer
import kotlinx.coroutines.CoroutineScope

typealias Config = JsonObject

inline fun <reified T> Config.get(path: String): T? {
    val value = JsonPointer.from("/$path").queryJson(this)

    return if (value is T?) {
        value
    } else {
        null
    }
}

object ConfigReader {
    fun read(vertx: Vertx): Config {
        val configFileStore = ConfigStoreOptions()
            .setType("file")
            .setFormat("hocon")
            .setOptional(true)
            .setConfig(
                JsonObject()
                    .put("path", System.getenv().getOrDefault("ENV_FILE", "./.env"))
            )
        val envVarsStore = ConfigStoreOptions()
            .setType("env")

        return ConfigRetriever.create(
            vertx,
            ConfigRetrieverOptions()
                .addStore(configFileStore)
                .addStore(envVarsStore)
                .setScanPeriod(0)
        )
            .config
            .toCompletionStage()
            .toCompletableFuture()
            .join()
    }
}

data class VerticleContext(
    val vertx: Vertx,
    val config: Config,
    val scope: CoroutineScope
)
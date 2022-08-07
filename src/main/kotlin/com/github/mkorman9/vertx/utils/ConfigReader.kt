package com.github.mkorman9.vertx.utils

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

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

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
            .setFormat("yaml")
            .setOptional(true)
            .setConfig(
                JsonObject()
                .put("path", System.getenv().getOrDefault("CONFIG_FILE", "./config.yml"))
            )
        val secretsFileStore = ConfigStoreOptions()
            .setType("file")
            .setFormat("yaml")
            .setOptional(true)
            .setConfig(
                JsonObject()
                .put("path", System.getenv().getOrDefault("SECRETS_FILE", "./secrets.yml"))
            )
        val envVarsStore = ConfigStoreOptions()
            .setType("env")

        return ConfigRetriever.create(vertx, ConfigRetrieverOptions()
            .addStore(configFileStore)
            .addStore(secretsFileStore)
            .addStore(envVarsStore)
            .setScanPeriod(0)
        )
            .setConfigurationProcessor { config ->
                val newConfig = mutableMapOf<String, Any>()

                config.map.forEach { entry ->
                    val key = entry.key.lowercase()
                    val value = entry.value

                    val splits = key.split("_")
                    if (splits.size == 1) {
                        newConfig[key] = value
                    } else {
                        var ptr = newConfig
                        splits.forEachIndexed { index, s ->
                            if (index < splits.size - 1) {
                                if (!ptr.containsKey(s)) {
                                    ptr[s] = mutableMapOf<String, Any>()
                                }

                                if (ptr[s] is Map<*, *>) {
                                    @Suppress("UNCHECKED_CAST")
                                    ptr = ptr[s] as MutableMap<String, Any>
                                }
                            } else {
                                ptr[s] = value
                            }
                        }
                    }
                }

                JsonObject(newConfig)
            }
            .config
            .toCompletionStage()
            .toCompletableFuture()
            .join()
    }
}

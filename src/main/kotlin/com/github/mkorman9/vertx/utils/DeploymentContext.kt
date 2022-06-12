package com.github.mkorman9.vertx.utils

import java.time.LocalDateTime

data class DeploymentContext(
    val version: String,
    val startupTime: LocalDateTime,
    val environment: String
) {
    companion object {
        private val instance =
            DeploymentContext(
                version = VersionReader.read(),
                startupTime = LocalDateTime.now(),
                environment = System.getenv("ENVIRONMENT_NAME") ?: "default"
            )

        fun create(): DeploymentContext {
            return instance
        }
    }
}

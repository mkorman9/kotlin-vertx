package com.github.mkorman9.vertx.utils

import java.time.LocalDateTime

data class DeploymentContext(
    val version: String,
    val startupTime: LocalDateTime,
    val environment: String
) {
    companion object {
        fun create(): DeploymentContext {
            return DeploymentContext(
                version = VersionReader.read(),
                startupTime = LocalDateTime.now(),
                environment = System.getenv("ENVIRONMENT_NAME") ?: "default"
            )
        }
    }
}

package com.github.mkorman9.vertx.utils

import java.io.IOException
import java.time.LocalDateTime
import java.util.jar.Manifest

class DeploymentInfo private constructor(
    val version: String,
    val startupTime: LocalDateTime,
    val environment: String,
    val profile: String
) {
    companion object {
        private const val DEFAULT_VERSION = "dev"
        private const val DEFAULT_ENVIRONMENT = "default"
        private const val DEFAULT_PROFILE = "local"

        private val instance =
            DeploymentInfo(
                version = readVersion() ?: DEFAULT_VERSION,
                startupTime = LocalDateTime.now(),
                environment = System.getenv("ENVIRONMENT_NAME") ?: DEFAULT_ENVIRONMENT,
                profile = System.getenv("PROFILE") ?: DEFAULT_PROFILE
            )

        fun get(): DeploymentInfo {
            return instance
        }

        private fun readVersion(): String? {
            try {
                val resources = DeploymentInfo::class.java.classLoader.getResources("META-INF/MANIFEST.MF")
                while (resources.hasMoreElements()) {
                    resources.nextElement().openStream().use { stream ->
                        val manifest = Manifest(stream)
                        val attributes = manifest.mainAttributes

                        return attributes.getValue("Version")
                    }
                }

                return null
            } catch (e: IOException) {
                throw e
            }
        }
    }
}

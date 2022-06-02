package com.github.mkorman9.vertx

import com.google.api.gax.core.CredentialsProvider
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.api.gax.core.NoCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
import java.io.ByteArrayInputStream

class GCPSettings(
    val projectId: String,
    val credentialsProvider: CredentialsProvider
) {
    companion object {
        private val log = LoggerFactory.getLogger(GCPSettings::class.java)

        suspend fun read(vertx: Vertx, config: JsonObject): GCPSettings {
            val projectId = config.getJsonObject("gcp")?.getString("projectId") ?: "stub-project-id"

            val credentialsConfig = config.getJsonObject("gcp")?.getJsonObject("credentials")
            val path = credentialsConfig?.getString("path") ?: ""

            if (path == "") {
                log.info("Empty gcp.credentials.path property, continuing without cloud integration")
                return GCPSettings(projectId, NoCredentialsProvider.create())
            }

            val content = try {
                vertx.fileSystem().readFile(path).await()
            } catch (e: Exception) {
                log.error("Failed to load GoogleCredentials, continuing", e)
                return GCPSettings(projectId, NoCredentialsProvider.create())
            }

            try {
                val credentials = GoogleCredentials.fromStream(ByteArrayInputStream(content.bytes))
                return GCPSettings(projectId, FixedCredentialsProvider.create(credentials))
            } catch (e: Exception) {
                log.error("Failed to parse GoogleCredentials, continuing", e)
                return GCPSettings(projectId, NoCredentialsProvider.create())
            }
        }
    }
}

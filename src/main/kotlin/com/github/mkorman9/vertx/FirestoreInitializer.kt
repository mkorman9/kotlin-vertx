package com.github.mkorman9.vertx

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import io.vertx.core.json.JsonObject

class FirestoreInitializer(
    private val config: JsonObject
) {
    fun initialize(): Firestore {
        val gcpConfig = config.getJsonObject("gcp")
        val projectId = gcpConfig?.getString("projectId") ?: "default-project-id"

        val emulatorConfig = gcpConfig?.getJsonObject("firestore")?.getJsonObject("emulator")
        val emulatorEnabled = emulatorConfig?.getBoolean("enabled") ?: false
        val emulatorAddress = emulatorConfig?.getString("address") ?: "localhost:8200"

        val builder = FirestoreOptions.newBuilder()
            .setProjectId(projectId)

        if (emulatorEnabled) {
            builder
                .setCredentials(FirestoreOptions.EmulatorCredentials())
                .setHost(emulatorAddress)
        } else {
            builder
                .setCredentials(GoogleCredentials.getApplicationDefault())
        }

        return builder
            .build()
            .service
    }
}

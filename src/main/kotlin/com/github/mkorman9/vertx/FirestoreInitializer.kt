package com.github.mkorman9.vertx

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import io.vertx.core.json.JsonObject

class FirestoreInitializer(
    private val gcpSettings: GCPSettings,
    private val config: JsonObject
) {
    fun initialize(): Firestore {
        val firestoreConfig = config.getJsonObject("gcp")?.getJsonObject("firestore")
        val emulatorConfig = firestoreConfig?.getJsonObject("emulator")
        val emulatorEnabled = emulatorConfig?.getBoolean("enabled") ?: false
        val emulatorAddress = emulatorConfig?.getString("address") ?: "localhost:8200"

        val builder = FirestoreOptions.newBuilder()
            .setProjectId(gcpSettings.projectId)

        if (emulatorEnabled) {
            builder
                .setCredentials(FirestoreOptions.EmulatorCredentials())
                .setHost(emulatorAddress)
        } else {
            builder
                .setCredentialsProvider(gcpSettings.credentialsProvider)
        }

        return builder
            .build()
            .service
    }
}

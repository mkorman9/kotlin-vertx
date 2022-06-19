package com.github.mkorman9.vertx.tools.firestore

import com.github.mkorman9.vertx.utils.Config
import com.github.mkorman9.vertx.utils.get
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions

class FirestoreInitializer {
    companion object {
        fun initialize(config: Config): Firestore {
            val projectId = config.get<String>("gcp.project")
            val emulatorEnabled = config.get<Boolean>("gcp.firestore.emulator.enabled") ?: false
            val emulatorAddress = config.get<String>("gcp.firestore.emulator.address") ?: "localhost:8200"

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
}

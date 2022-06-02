package com.github.mkorman9.vertx

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions

class FirestoreInitializer(
    private val gcpSettings: GCPSettings
) {
    fun initialize(): Firestore {
        return FirestoreOptions.newBuilder()
            .setProjectId(gcpSettings.projectId)
            .setCredentials(FirestoreOptions.EmulatorCredentials())
            .setHost("localhost:8200")
            .build()
            .service
    }
}

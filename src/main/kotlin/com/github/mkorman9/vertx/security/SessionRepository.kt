package com.github.mkorman9.vertx.security

import com.google.cloud.firestore.Firestore
import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.core.Future
import io.vertx.core.Vertx
import java.time.LocalDateTime
import java.time.ZoneOffset

@Singleton
class SessionRepository @Inject constructor(
    private val vertx: Vertx,
    private val firestore: Firestore
) {
    companion object {
        private const val SESSIONS_COLLECTION = "sessions"
    }

    fun findByToken(token: String): Future<Session?> {
        return vertx.executeBlocking<Session?> { call ->
            val docs = firestore.collection(SESSIONS_COLLECTION)
                .whereEqualTo("token", token)
                .whereGreaterThan("expiresAt", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                .get()
                .get()
                .documents

            if (docs.isEmpty()) {
                call.complete(null)
            } else {
                call.complete(docs[0].toObject(Session::class.java))
            }
        }
    }

    fun add(sessionObject: Session): Future<Session> {
        return vertx.executeBlocking { call ->
            firestore.collection(SESSIONS_COLLECTION)
                .document(sessionObject.id)
                .set(sessionObject)
                .get()

            call.complete(sessionObject)
        }
    }

    fun refresh(sessionObject: Session): Future<Session> {
        if (sessionObject.duration == null) {
            return Future.succeededFuture(sessionObject)
        }

        sessionObject.expiresAt = LocalDateTime.now()
            .plusSeconds(sessionObject.duration!!.toLong())
            .toEpochSecond(ZoneOffset.UTC)

        return vertx.executeBlocking { call ->
            firestore.collection(SESSIONS_COLLECTION)
                .document(sessionObject.id)
                .set(sessionObject)
                .get()

            call.complete(sessionObject)
        }
    }

    fun delete(sessionObject: Session): Future<Void> {
        return vertx.executeBlocking { call ->
            firestore.collection(SESSIONS_COLLECTION)
                .document(sessionObject.id)
                .delete()
                .get()

            call.complete()
        }
    }

    fun deleteExpired(): Future<Int> {
        return vertx.executeBlocking { call ->
            val docs = firestore.collection(SESSIONS_COLLECTION)
                .whereLessThan("expiresAt", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                .get()
                .get()
                .documents
            val count = docs.size

            val batch = firestore.batch()
            docs.forEach {
                batch.delete(it.reference)
            }

            batch.commit()
                .get()

            call.complete(count)
        }
    }
}

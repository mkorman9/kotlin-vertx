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
    private val firestore: Firestore,
    private val accountRepository: AccountRepository
) {
    companion object {
        private const val SESSIONS_COLLECTION = "sessions"
    }

    fun findByToken(vertx: Vertx, token: String): Future<Session?> {
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
                val sessionDocument = docs[0].toObject(SessionDocument::class.java)

                accountRepository.findById(vertx, sessionDocument.accountId)
                    .onSuccess { account ->
                        if (account != null) {
                            call.complete(sessionDocument.toSession(account))
                        } else {
                            call.complete(null)
                        }
                    }
                    .onFailure { cause -> call.fail(cause) }
            }
        }
    }

    fun add(vertx: Vertx, session: Session): Future<Session> {
        return vertx.executeBlocking { call ->
            firestore.collection(SESSIONS_COLLECTION)
                .document(session.id.toString())
                .set(SessionDocument.fromSession(session))
                .get()

            call.complete(session)
        }
    }

    fun refresh(vertx: Vertx, session: Session): Future<Session> {
        if (session.duration == null) {
            return Future.succeededFuture(session)
        }

        session.expiresAt = LocalDateTime.now().plusSeconds(session.duration!!.toLong())

        return vertx.executeBlocking { call ->
            firestore.collection(SESSIONS_COLLECTION)
                .document(session.id.toString())
                .set(SessionDocument.fromSession(session))
                .get()

            call.complete(session)
        }
    }

    fun delete(vertx: Vertx, sessionObject: Session): Future<Void> {
        return vertx.executeBlocking { call ->
            firestore.collection(SESSIONS_COLLECTION)
                .document(sessionObject.id.toString())
                .delete()
                .get()

            call.complete()
        }
    }

    fun deleteExpired(vertx: Vertx): Future<Int> {
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

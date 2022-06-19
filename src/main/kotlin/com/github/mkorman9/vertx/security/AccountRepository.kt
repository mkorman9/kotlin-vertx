package com.github.mkorman9.vertx.security

import com.google.cloud.firestore.FieldPath
import com.google.cloud.firestore.Firestore
import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.core.Future
import io.vertx.core.Vertx

@Singleton
class AccountRepository @Inject constructor(
    private val firestore: Firestore
) {
    companion object {
        private const val ACCOUNTS_COLLECTION = "accounts"
    }

    fun findById(vertx: Vertx, id: String): Future<Account?> {
        return vertx.executeBlocking<Account?> { call ->
            val doc = firestore.collection(ACCOUNTS_COLLECTION)
                .document(id)
                .get()
                .get()
            val accountDocument = doc.toObject(AccountDocument::class.java)

            if (accountDocument != null && !accountDocument.deleted) {
                call.complete(accountDocument.toAccount())
            } else {
                call.complete(null)
            }
        }
    }

    fun findByCredentialsEmail(vertx: Vertx, email: String): Future<Account?> {
        return vertx.executeBlocking<Account?> { call ->
            val docs = firestore.collection(ACCOUNTS_COLLECTION)
                .whereEqualTo(FieldPath.of("credentials", "email"), email)
                .whereEqualTo("deleted", false)
                .get()
                .get()
                .documents

            if (docs.isEmpty()) {
                call.complete(null)
            } else {
                val accountDocument = docs[0].toObject(AccountDocument::class.java)
                call.complete(accountDocument.toAccount())
            }
        }
    }
}

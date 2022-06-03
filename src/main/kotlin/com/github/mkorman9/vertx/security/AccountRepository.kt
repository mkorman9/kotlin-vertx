package com.github.mkorman9.vertx.security

import com.google.cloud.firestore.FieldPath
import com.google.cloud.firestore.Firestore
import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.core.Future
import io.vertx.core.Vertx

@Singleton
class AccountRepository @Inject constructor(
    private val vertx: Vertx,
    private val firestore: Firestore
) {
    companion object {
        private const val ACCOUNTS_COLLECTION = "accounts"
    }

    fun findById(id: String): Future<Account?> {
        return vertx.executeBlocking<Account?> { call ->
            val doc = firestore.collection(ACCOUNTS_COLLECTION)
                .document(id)
                .get()
                .get()

            call.complete(doc.toObject(Account::class.java))
        }
    }

    fun findByCredentialsEmail(email: String): Future<Account?> {
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
                val account = docs[0].toObject(Account::class.java)
                call.complete(account)
            }
        }
    }
}

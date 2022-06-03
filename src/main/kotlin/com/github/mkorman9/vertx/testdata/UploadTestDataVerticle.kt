package com.github.mkorman9.vertx.testdata

import com.fasterxml.jackson.core.type.TypeReference
import com.github.mkorman9.vertx.BootstrapVerticle
import com.github.mkorman9.vertx.client.Client
import com.github.mkorman9.vertx.security.Account
import com.github.mkorman9.vertx.utils.DeployVerticle
import com.google.cloud.firestore.Firestore
import com.google.inject.Injector
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.config.ConfigRetriever
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import java.io.IOException

@DeployVerticle
class UploadTestDataVerticle(
    passedInjector: Injector? = null
) : CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(UploadTestDataVerticle::class.java)

    private val injector: Injector = passedInjector ?: BootstrapVerticle.injector
    private val firestore = injector.getInstance<Firestore>()

    override suspend fun start() {
        val configRetriever = injector.getInstance<ConfigRetriever>()
        val config = configRetriever.config.await()

        val uploadTestData = config.getJsonObject("testdata")?.getBoolean("upload") ?: false

        if (uploadTestData) {
            CompositeFuture.all(uploadAccounts(), uploadClients()).await()
            log.info("UploadTestDataVerticle has finished uploading test data")
        }
    }

    private fun uploadAccounts(): Future<Void> {
        return vertx.executeBlocking { call ->
            try {
                val resources = UploadTestDataVerticle::class.java.classLoader.getResources("testdata/accounts.json")
                while (resources.hasMoreElements()) {
                    resources.nextElement().openStream().use { stream ->
                        val bytes = stream.readAllBytes()
                        val accounts =
                            DatabindCodec.mapper().readValue(bytes, object : TypeReference<List<Account>>() {})

                        val batch = firestore.batch()

                        accounts.forEach {
                            batch.set(
                                firestore.collection("accounts").document(it.id),
                                it
                            )
                        }

                        batch.commit()
                            .get()

                        call.complete()
                    }
                }
            } catch (e: IOException) {
                call.fail(e)
            }
        }
    }

    private fun uploadClients(): Future<Void> {
        return vertx.executeBlocking { call ->
            try {
                val resources = UploadTestDataVerticle::class.java.classLoader.getResources("testdata/clients.json")
                while (resources.hasMoreElements()) {
                    resources.nextElement().openStream().use { stream ->
                        val bytes = stream.readAllBytes()
                        val clients =
                            DatabindCodec.mapper().readValue(bytes, object : TypeReference<List<Client>>() {})

                        val batch = firestore.batch()

                        clients.forEach {
                            batch.set(
                                firestore.collection("clients").document(it.id),
                                it
                            )
                        }

                        batch.commit()
                            .get()

                        call.complete()
                    }
                }
            } catch (e: IOException) {
                call.fail(e)
            }
        }
    }
}

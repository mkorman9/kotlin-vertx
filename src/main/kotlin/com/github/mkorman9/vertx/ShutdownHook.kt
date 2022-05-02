package com.github.mkorman9.vertx

import io.vertx.core.Vertx

class ShutdownHook {
    companion object {
        private const val CHECK_INTERVAL_MS: Long = 100

        fun register(vertx: Vertx) {
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    val closing = vertx.close()
                        .toCompletionStage()
                        .toCompletableFuture()

                    while (true) {
                        if (closing.isDone || closing.isCancelled) {
                            break
                        }

                        sleep(CHECK_INTERVAL_MS)
                    }
                }
            })
        }
    }
}

package com.github.mkorman9.vertx

import io.vertx.core.Vertx

class ShutdownHook {
    companion object {
        fun register(vertx: Vertx) {
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    vertx.close()
                        .toCompletionStage()
                        .toCompletableFuture()
                        .join()
                }
            })
        }
    }
}

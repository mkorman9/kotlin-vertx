package com.github.mkorman9.vertx

import io.vertx.core.Vertx

class ShutdownHook {
    companion object {
        fun register(vertx: Vertx, bootstrapper: AppBootstrapper) {
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    vertx.close()
                        .toCompletionStage()
                        .toCompletableFuture()
                        .join()

                    bootstrapper.shutdown()
                }
            })
        }
    }
}

package com.github.mkorman9.vertx.utils

object ShutdownHook {
    fun register(r: Runnable) {
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                r.run()
            }
        })
    }
}

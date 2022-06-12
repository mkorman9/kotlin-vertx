package com.github.mkorman9.vertx.utils

class ShutdownHook {
    companion object {
        fun register(r: Runnable) {
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    r.run()
                }
            })
        }
    }
}

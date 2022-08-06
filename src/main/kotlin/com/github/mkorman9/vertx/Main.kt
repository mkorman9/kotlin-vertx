package com.github.mkorman9.vertx

fun main() {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
    System.setProperty("user.timezone", "UTC")

    Application.bootstrap()
}

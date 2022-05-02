package com.github.mkorman9.vertx

import io.vertx.core.Vertx

fun main() {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
    System.setProperty("user.timezone", "UTC")

    val vertx = Vertx.vertx()
    vertx.deployVerticle(BootstrapVerticle())
}

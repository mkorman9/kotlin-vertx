package com.github.mkorman9.vertx

import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.micrometer.MicrometerMetricsOptions
import io.vertx.micrometer.VertxPrometheusOptions

fun main() {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
    System.setProperty("user.timezone", "UTC")

    val vertx = Vertx.vertx(VertxOptions()
        .setMetricsOptions(
            MicrometerMetricsOptions()
                .setPrometheusOptions(VertxPrometheusOptions().setEnabled(true))
                .setEnabled(true)
        )
    )

    vertx.deployVerticle(BootstrapVerticle::class.java.name)
}

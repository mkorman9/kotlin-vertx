package com.github.mkorman9.vertx.utils

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DeployVerticle(
    val configKey: String = ""
)

package com.github.mkorman9.vertx.utils

import java.security.SecureRandom

class SecureRandomGenerator {
    companion object {
        private val random = SecureRandom.getInstance(getPRNGAlgorithm())
        private const val charset = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

        fun generate(length: Long): String {
            return random
                .ints(length, 0, charset.length)
                .mapToObj { charset[it] }
                .collect({ java.lang.StringBuilder() }, StringBuilder::append, StringBuilder::append)
                .toString()
        }

        private fun getPRNGAlgorithm(): String {
            return if (System.getProperty("os.name").lowercase().contains("win")) {
                "SHA1PRNG"
            } else {
                "NativePRNGNonBlocking"
            }
        }
    }
}

package com.github.mkorman9.vertx.utils

import java.io.IOException
import java.util.jar.Manifest

class VersionReader {
    companion object {
        private const val DEFAULT_VERSION = "dev"

        fun read(): String {
            try {
                val resources = VersionReader::class.java.classLoader.getResources("META-INF/MANIFEST.MF")
                while (resources.hasMoreElements()) {
                    resources.nextElement().openStream().use { stream ->
                        val manifest = Manifest(stream)
                        val attributes = manifest.mainAttributes

                        return attributes.getValue("Version") ?: DEFAULT_VERSION
                    }
                }

                return DEFAULT_VERSION
            } catch (e: IOException) {
                throw e
            }
        }
    }
}

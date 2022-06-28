package com.github.mkorman9.vertx.utils

import org.reflections.Reflections

object ReflectionsUtils {
    fun findClasses(packageName: String, vararg annotations: Class<out Annotation>): List<Class<Any>> {
        val packageReflections = Reflections(packageName)

        return annotations
            .flatMap {
                @Suppress("UNCHECKED_CAST")
                packageReflections.getTypesAnnotatedWith(it) as Set<Class<Any>>
            }
    }
}

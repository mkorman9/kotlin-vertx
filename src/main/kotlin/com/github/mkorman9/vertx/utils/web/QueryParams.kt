package com.github.mkorman9.vertx.utils.web

import io.vertx.ext.web.RoutingContext

typealias QueryParamsParsingRules = Map<String, (value: String?) -> Any?>

class QueryParamValues private constructor(
    private val values: Map<String, Any>
) {
    companion object {
        fun parse(ctx: RoutingContext, rules: QueryParamsParsingRules): QueryParamValues {
            val values = mutableMapOf<String, Any>()

            rules.forEach { (param, func) ->
                val value = ctx.request().getParam(param)

                val transformed = func(value)
                if (transformed != null) {
                    values[param] = transformed
                }
            }

            return QueryParamValues(values)
        }
    }

    fun <T> get(param: String): T? {
        @Suppress("UNCHECKED_CAST")
        return values[param] as T?
    }

    fun <T> mustGet(param: String): T {
        @Suppress("UNCHECKED_CAST")
        return values[param] as T
    }
}

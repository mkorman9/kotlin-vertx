package com.github.mkorman9.vertx.security

import io.vertx.ext.web.RoutingContext

interface AuthorizationMiddleware {
    fun authorize(ctx: RoutingContext, allowedRoles: Set<String>? = null)
    fun getActiveSession(ctx: RoutingContext): Session
}

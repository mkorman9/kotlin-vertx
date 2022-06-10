package com.github.mkorman9.vertx.security

import io.vertx.ext.web.RoutingContext

class AuthorizationMiddlewareMock(
    private val mockSessionProvider: MockSessionProvider
) : AuthorizationMiddleware {
    override fun authorize(ctx: RoutingContext, allowedRoles: Set<String>?) {
        ctx.next()
    }

    override fun getActiveSession(ctx: RoutingContext): Session {
        return mockSessionProvider.getSession()
    }
}

class MockSessionProvider(
    private val mockSession: Session
) {
    fun getSession(): Session {
        return mockSession
    }
}

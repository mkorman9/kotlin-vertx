package com.github.mkorman9.vertx.security

import io.vertx.ext.web.RoutingContext

data class MockCredentials(
    val session: Session,
    val account: Account
)

class AuthorizationMiddlewareMock(
    private val mockCredentialsProvider: MockCredentialsProvider
) : AuthorizationMiddleware {
    override fun authorize(ctx: RoutingContext, allowedRoles: Set<String>?) {
        ctx.next()
    }

    override fun getActiveSession(ctx: RoutingContext): Session {
        return mockCredentialsProvider.getCredentials().session
    }

    override fun getActiveAccount(ctx: RoutingContext): Account {
        return mockCredentialsProvider.getCredentials().account
    }
}

class MockCredentialsProvider(
    private val mockSession: Session,
    private val mockAccount: Account
) {
    fun getCredentials(): MockCredentials {
        return MockCredentials(mockSession, mockAccount)
    }
}

package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.data.auth.AuthFailureKind
import com.kzzz3.argus.lens.data.auth.AuthRepository
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.data.auth.AuthSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSessionCoordinatorTest {
    @Test
    fun refreshSession_withoutRefreshTokenReturnsUnauthorizedFailureAndKeepsSession() = runBlocking {
        val session = AppSessionState(
            isAuthenticated = true,
            accountId = "tester",
            displayName = "Tester",
            accessToken = "access-token",
            refreshToken = "",
        )
        val coordinator = AppSessionCoordinator(FakeAuthRepository())

        val result = coordinator.refreshSession(session)

        assertEquals(session, result.session)
        val failure = result.repositoryResult as AuthRepositoryResult.Failure
        assertEquals(AuthFailureKind.UNAUTHORIZED, failure.kind)
        assertEquals("INVALID_CREDENTIALS", failure.code)
    }

    @Test
    fun refreshSessionWithToken_updatesSessionAndKeepsFallbackRefreshTokenWhenResponseIsBlank() = runBlocking {
        val session = AppSessionState(
            isAuthenticated = true,
            accountId = "tester",
            displayName = "Tester",
            accessToken = "old-access",
            refreshToken = "persisted-refresh",
        )
        val repository = FakeAuthRepository(
            refreshResult = AuthRepositoryResult.Success(
                AuthSession(
                    accountId = "tester",
                    displayName = "Tester",
                    accessToken = "new-access",
                    refreshToken = "",
                    message = "refreshed",
                )
            )
        )
        val coordinator = AppSessionCoordinator(repository)

        val result = coordinator.refreshSessionWithToken(session, "one-shot-refresh")

        assertTrue(result.session.isAuthenticated)
        assertEquals("new-access", result.session.accessToken)
        assertEquals("persisted-refresh", result.session.refreshToken)
        assertEquals(listOf("one-shot-refresh"), repository.refreshRequests)
    }

    @Test
    fun refreshSession_keepsCurrentSessionOnRepositoryFailure() = runBlocking {
        val session = AppSessionState(
            isAuthenticated = true,
            accountId = "tester",
            displayName = "Tester",
            accessToken = "old-access",
            refreshToken = "persisted-refresh",
        )
        val coordinator = AppSessionCoordinator(
            FakeAuthRepository(
                refreshResult = AuthRepositoryResult.Failure(
                    code = "NETWORK",
                    message = "offline",
                    kind = AuthFailureKind.NETWORK,
                )
            )
        )

        val result = coordinator.refreshSession(session)

        assertFalse(result.repositoryResult is AuthRepositoryResult.Success)
        assertEquals(session, result.session)
    }

    private class FakeAuthRepository(
        private val refreshResult: AuthRepositoryResult = AuthRepositoryResult.Failure(
            code = "UNUSED",
            message = "unused",
            kind = AuthFailureKind.SERVER,
        ),
    ) : AuthRepository {
        val refreshRequests = mutableListOf<String>()

        override suspend fun restoreSession(accessToken: String): AuthRepositoryResult = refreshResult

        override suspend fun refreshSession(refreshToken: String): AuthRepositoryResult {
            refreshRequests.add(refreshToken)
            return refreshResult
        }

        override suspend fun login(account: String, password: String): AuthRepositoryResult = refreshResult

        override suspend fun register(displayName: String, account: String, password: String): AuthRepositoryResult = refreshResult
    }
}

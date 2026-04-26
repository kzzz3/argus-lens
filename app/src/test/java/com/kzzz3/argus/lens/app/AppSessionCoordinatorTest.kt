package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.model.session.AppSessionState
import com.kzzz3.argus.lens.data.auth.AuthFailureKind
import com.kzzz3.argus.lens.data.auth.AuthRepository
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.data.auth.AuthSession
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.data.session.SessionRepository
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
        )
        val sessionRepository = FakeSessionRepository(
            session = session,
            credentials = SessionCredentials(accessToken = "access-token"),
        )
        val coordinator = AppSessionCoordinator(FakeAuthRepository(), sessionRepository)

        val result = coordinator.refreshSession(session)

        assertEquals(session, result.session)
        assertEquals(SessionCredentials(accessToken = "access-token"), result.credentials)
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
        )
        val sessionRepository = FakeSessionRepository(
            session = session,
            credentials = SessionCredentials(accessToken = "old-access", refreshToken = "persisted-refresh"),
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
        val coordinator = AppSessionCoordinator(repository, sessionRepository)

        val result = coordinator.refreshSessionWithToken(session, "one-shot-refresh")

        assertTrue(result.session.isAuthenticated)
        assertEquals("tester", result.session.accountId)
        assertEquals("new-access", result.credentials.accessToken)
        assertEquals("persisted-refresh", result.credentials.refreshToken)
        assertEquals(listOf("one-shot-refresh"), repository.refreshRequests)
    }

    @Test
    fun refreshSession_keepsCurrentSessionOnRepositoryFailure() = runBlocking {
        val session = AppSessionState(
            isAuthenticated = true,
            accountId = "tester",
            displayName = "Tester",
        )
        val credentials = SessionCredentials(accessToken = "old-access", refreshToken = "persisted-refresh")
        val coordinator = AppSessionCoordinator(
            FakeAuthRepository(
                refreshResult = AuthRepositoryResult.Failure(
                    code = "NETWORK",
                    message = "offline",
                    kind = AuthFailureKind.NETWORK,
                )
            ),
            FakeSessionRepository(session, credentials),
        )

        val result = coordinator.refreshSession(session)

        assertFalse(result.repositoryResult is AuthRepositoryResult.Success)
        assertEquals(session, result.session)
        assertEquals(credentials, result.credentials)
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

    private class FakeSessionRepository(
        private var session: AppSessionState,
        private var credentials: SessionCredentials,
    ) : SessionRepository {
        override suspend fun loadSession(): AppSessionState = session

        override suspend fun loadCredentials(): SessionCredentials = credentials

        override suspend fun saveSession(state: AppSessionState, credentials: SessionCredentials) {
            this.session = state
            this.credentials = credentials
        }

        override suspend fun clearSession() {
            session = AppSessionState()
            credentials = SessionCredentials()
        }
    }
}

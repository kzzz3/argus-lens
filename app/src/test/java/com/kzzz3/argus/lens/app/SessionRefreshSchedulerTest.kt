package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.model.session.AppSessionState
import com.kzzz3.argus.lens.core.data.auth.AuthFailureKind
import com.kzzz3.argus.lens.core.data.auth.AuthRepository
import com.kzzz3.argus.lens.core.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.core.data.auth.AuthSession
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.session.SessionCredentialsStore
import com.kzzz3.argus.lens.session.SessionRefreshOutcome
import com.kzzz3.argus.lens.session.SessionRefreshScheduler
import com.kzzz3.argus.lens.session.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRefreshSchedulerTest {
    @Test
    fun refreshOnce_updatesSessionAndCredentialsOnSuccess() = runBlocking {
        val initialSession = AppSessionState(isAuthenticated = true, accountId = "tester", displayName = "Tester")
        val sessionRepository = FakeSessionRepository(
            session = initialSession,
            credentials = SessionCredentials("old-access", "old-refresh"),
        )
        val scheduler = SessionRefreshScheduler(
            scope = CoroutineScope(Dispatchers.Unconfined),
            sessionRefresher = AppSessionRefresher(
                authRepository = FakeAuthRepository(
                    result = AuthRepositoryResult.Success(
                        AuthSession("tester", "Updated Tester", "new-access", "new-refresh", "ok")
                    )
                ),
                sessionRepository = sessionRepository,
            ),
            credentialsStore = SessionCredentialsStore(SessionCredentials("old-access", "old-refresh")),
        )
        var session = initialSession

        val result = scheduler.refreshOnce(
            session = session,
            setSession = { session = it },
        )

        assertTrue(result is SessionRefreshOutcome.Success)
        assertEquals("Updated Tester", session.displayName)
        assertEquals("new-access", scheduler.currentCredentials.accessToken)
        assertEquals("new-refresh", scheduler.currentCredentials.refreshToken)
        scheduler.cancel()
    }

    @Test
    fun refreshOnce_returnsUnauthorizedWhenRefreshTokenMissing() = runBlocking {
        val scheduler = SessionRefreshScheduler(
            scope = CoroutineScope(Dispatchers.Unconfined),
            sessionRefresher = AppSessionRefresher(
                authRepository = FakeAuthRepository(),
                sessionRepository = FakeSessionRepository(
                    AppSessionState(isAuthenticated = true, accountId = "tester", displayName = "Tester"),
                    SessionCredentials(),
                ),
            ),
            credentialsStore = SessionCredentialsStore(),
        )

        val result = scheduler.refreshOnce(
            session = AppSessionState(isAuthenticated = true, accountId = "tester", displayName = "Tester"),
            setSession = {},
        )

        val failure = result as SessionRefreshOutcome.Failure
        assertTrue(failure.isUnauthorized)
        assertFalse(scheduler.currentCredentials.hasRefreshToken)
        scheduler.cancel()
    }

    private class FakeAuthRepository(
        private val result: AuthRepositoryResult = AuthRepositoryResult.Failure(
            code = "INVALID_CREDENTIALS",
            message = "missing",
            kind = AuthFailureKind.UNAUTHORIZED,
        ),
    ) : AuthRepository {
        override suspend fun restoreSession(accessToken: String): AuthRepositoryResult = result
        override suspend fun refreshSession(refreshToken: String): AuthRepositoryResult = result
        override suspend fun login(account: String, password: String): AuthRepositoryResult = result
        override suspend fun register(displayName: String, account: String, password: String): AuthRepositoryResult = result
    }

    private class FakeSessionRepository(
        private var session: AppSessionState,
        private var credentials: SessionCredentials,
    ) : SessionRepository {
        override suspend fun loadSession(): AppSessionState = session
        override suspend fun loadCredentials(): SessionCredentials = credentials
        override suspend fun saveSession(state: AppSessionState, credentials: SessionCredentials) {
            session = state
            this.credentials = credentials
        }
        override suspend fun clearSession() {
            session = AppSessionState()
            credentials = SessionCredentials()
        }
    }
}

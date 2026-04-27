package com.kzzz3.argus.lens.data.session

import com.kzzz3.argus.lens.model.session.AppSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalSessionStoreDelegationTest {
    @Test
    fun sessionRepositoryDelegatesIdentityAndCredentialsOperationsToSeparateStores() = runBlocking {
        val initialSession = AppSessionState(
            isAuthenticated = true,
            accountId = "argus_tester",
            displayName = "Argus Tester",
        )
        val initialCredentials = SessionCredentials(
            accessToken = "access-token",
            refreshToken = "refresh-token",
        )
        val sessionStateStore = RecordingLocalSessionStateStore(initialSession)
        val credentialsStore = RecordingLocalSessionCredentialsStore(initialCredentials)
        val repository = LocalSessionStore(sessionStateStore, credentialsStore)

        assertEquals(initialSession, repository.loadSession())
        assertEquals(initialCredentials, repository.loadCredentials())

        val updatedSession = initialSession.copy(displayName = "Argus Updated")
        val updatedCredentials = initialCredentials.copy(accessToken = "updated-access-token")

        repository.saveSession(updatedSession, updatedCredentials)

        assertEquals(updatedSession, sessionStateStore.savedSession)
        assertEquals(updatedCredentials, credentialsStore.savedCredentials)

        repository.clearSession()

        assertEquals(1, sessionStateStore.clearCount)
        assertEquals(1, credentialsStore.clearCount)
    }

    private class RecordingLocalSessionStateStore(
        private var session: AppSessionState,
    ) : LocalSessionStateStore {
        var savedSession: AppSessionState? = null
            private set
        var clearCount: Int = 0
            private set

        override suspend fun loadSession(): AppSessionState = session

        override suspend fun saveSession(state: AppSessionState) {
            savedSession = state
            session = state
        }

        override suspend fun clearSession() {
            clearCount += 1
            session = AppSessionState()
        }
    }

    private class RecordingLocalSessionCredentialsStore(
        private var credentials: SessionCredentials,
    ) : LocalSessionCredentialsStore {
        var savedCredentials: SessionCredentials? = null
            private set
        var clearCount: Int = 0
            private set

        override fun loadCredentials(): SessionCredentials = credentials

        override fun saveCredentials(credentials: SessionCredentials) {
            savedCredentials = credentials
            this.credentials = credentials
        }

        override fun clearCredentials() {
            clearCount += 1
            credentials = SessionCredentials()
        }
    }
}

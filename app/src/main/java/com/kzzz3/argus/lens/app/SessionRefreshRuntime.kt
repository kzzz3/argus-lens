package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.data.auth.AuthFailureKind
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.data.session.SessionCredentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SessionRefreshRuntime(
    private val scope: CoroutineScope,
    private val appSessionCoordinator: AppSessionCoordinator,
    private val credentialsStore: SessionCredentialsStore,
    private val refreshIntervalMillis: Long = 60 * 60 * 1_000L,
) {
    private var refreshJob: Job? = null

    val currentCredentials: SessionCredentials
        get() = credentialsStore.current

    suspend fun refreshOnce(
        session: AppSessionState,
        setSession: (AppSessionState) -> Unit,
    ): AuthRepositoryResult {
        val refreshResult = appSessionCoordinator.refreshSession(session)
        credentialsStore.update(refreshResult.credentials)
        setSession(refreshResult.session)
        return refreshResult.repositoryResult
    }

    fun startLoopIfNeeded(
        getSession: () -> AppSessionState,
        getConnectionState: () -> ConversationRealtimeConnectionState,
        setSession: (AppSessionState) -> Unit,
        onUnauthorized: () -> Unit,
    ) {
        if (refreshJob?.isActive == true) return
        val initialSession = getSession()
        if (!initialSession.isAuthenticated || !credentialsStore.current.hasRefreshToken) return
        refreshJob = scope.launch {
            while (getSession().isAuthenticated && getConnectionState() == ConversationRealtimeConnectionState.LIVE) {
                delay(refreshIntervalMillis)
                if (!getSession().isAuthenticated || !credentialsStore.current.hasRefreshToken) {
                    break
                }
                when (val result = refreshOnce(getSession(), setSession)) {
                    is AuthRepositoryResult.Success -> Unit
                    is AuthRepositoryResult.Failure -> {
                        if (result.kind == AuthFailureKind.UNAUTHORIZED) {
                            onUnauthorized()
                            break
                        }
                    }
                }
            }
            refreshJob = null
        }
    }

    fun cancel() {
        refreshJob?.cancel()
        refreshJob = null
    }
}

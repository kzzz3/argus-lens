package com.kzzz3.argus.lens.session

import com.kzzz3.argus.lens.model.session.AppSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface SessionRefresher {
    suspend fun refreshSession(session: AppSessionState): SessionRefreshResult
}

data class SessionRefreshResult(
    val outcome: SessionRefreshOutcome,
    val session: AppSessionState,
    val credentials: SessionCredentials,
)

sealed interface SessionRefreshOutcome {
    data object Success : SessionRefreshOutcome

    data class Failure(
        val isUnauthorized: Boolean,
        val code: String = "",
        val message: String = "",
    ) : SessionRefreshOutcome
}

class SessionRefreshScheduler(
    private val scope: CoroutineScope,
    private val sessionRefresher: SessionRefresher,
    private val credentialsStore: SessionCredentialsStore,
    private val refreshIntervalMillis: Long = 60 * 60 * 1_000L,
) {
    private var refreshJob: Job? = null

    val currentCredentials: SessionCredentials
        get() = credentialsStore.current

    suspend fun refreshOnce(
        session: AppSessionState,
        setSession: (AppSessionState) -> Unit,
    ): SessionRefreshOutcome {
        val refreshResult = sessionRefresher.refreshSession(session)
        credentialsStore.update(refreshResult.credentials)
        setSession(refreshResult.session)
        return refreshResult.outcome
    }

    fun startLoopIfNeeded(
        getSession: () -> AppSessionState,
        isRefreshLoopActive: () -> Boolean,
        setSession: (AppSessionState) -> Unit,
        onUnauthorized: () -> Unit,
    ) {
        if (refreshJob?.isActive == true) return
        val initialSession = getSession()
        if (!initialSession.isAuthenticated || !credentialsStore.current.hasRefreshToken) return
        refreshJob = scope.launch {
            while (getSession().isAuthenticated && isRefreshLoopActive()) {
                delay(refreshIntervalMillis)
                if (!getSession().isAuthenticated || !credentialsStore.current.hasRefreshToken) {
                    break
                }
                when (val outcome = refreshOnce(getSession(), setSession)) {
                    SessionRefreshOutcome.Success -> Unit
                    is SessionRefreshOutcome.Failure -> {
                        if (outcome.isUnauthorized) {
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

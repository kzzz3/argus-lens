package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.core.data.auth.AuthFailureKind
import com.kzzz3.argus.lens.core.data.auth.AuthRepository
import com.kzzz3.argus.lens.core.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.app.state.createSessionCredentialsFromAuthSession
import com.kzzz3.argus.lens.app.state.createSessionFromAuthSession
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.session.SessionRefreshOutcome
import com.kzzz3.argus.lens.session.SessionRefreshResult
import com.kzzz3.argus.lens.session.SessionRefresher
import com.kzzz3.argus.lens.session.SessionRepository
import com.kzzz3.argus.lens.model.session.AppSessionState

class AppSessionRefresher(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) : SessionRefresher {
    override suspend fun refreshSession(
        session: AppSessionState,
    ): SessionRefreshResult {
        val currentCredentials = sessionRepository.loadCredentials()
        val refreshToken = currentCredentials.refreshToken
        if (refreshToken.isBlank()) {
            val failure = AuthRepositoryResult.Failure(
                code = "INVALID_CREDENTIALS",
                message = "Refresh token is missing.",
                kind = AuthFailureKind.UNAUTHORIZED,
            )
            return SessionRefreshResult(
                outcome = failure.toSessionRefreshOutcome(),
                session = session,
                credentials = currentCredentials,
            )
        }
        return applyRefreshResult(
            currentSession = session,
            currentCredentials = currentCredentials,
            repositoryResult = authRepository.refreshSession(refreshToken),
        )
    }

    suspend fun refreshSessionWithToken(
        session: AppSessionState,
        refreshToken: String,
    ): SessionRefreshResult {
        val currentCredentials = sessionRepository.loadCredentials()
        return applyRefreshResult(
            currentSession = session,
            currentCredentials = currentCredentials,
            repositoryResult = authRepository.refreshSession(refreshToken),
        )
    }

    private fun applyRefreshResult(
        currentSession: AppSessionState,
        currentCredentials: SessionCredentials,
        repositoryResult: AuthRepositoryResult,
    ): SessionRefreshResult {
        val nextSession = if (repositoryResult is AuthRepositoryResult.Success) {
            createSessionFromAuthSession(
                session = repositoryResult.session,
            )
        } else {
            currentSession
        }
        val nextCredentials = if (repositoryResult is AuthRepositoryResult.Success) {
            createSessionCredentialsFromAuthSession(
                session = repositoryResult.session,
                fallbackRefreshToken = currentCredentials.refreshToken,
            )
        } else {
            currentCredentials
        }
        return SessionRefreshResult(
            outcome = repositoryResult.toSessionRefreshOutcome(),
            session = nextSession,
            credentials = nextCredentials,
        )
    }
}

private fun AuthRepositoryResult.toSessionRefreshOutcome(): SessionRefreshOutcome {
    return when (this) {
        is AuthRepositoryResult.Success -> SessionRefreshOutcome.Success
        is AuthRepositoryResult.Failure -> SessionRefreshOutcome.Failure(
            isUnauthorized = kind == AuthFailureKind.UNAUTHORIZED,
            code = code.orEmpty(),
            message = message,
        )
    }
}

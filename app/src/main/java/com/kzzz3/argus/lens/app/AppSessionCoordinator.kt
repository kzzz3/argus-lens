package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.data.auth.AuthFailureKind
import com.kzzz3.argus.lens.data.auth.AuthRepository
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult

class AppSessionCoordinator(
    private val authRepository: AuthRepository,
) {
    suspend fun refreshSession(
        session: AppSessionState,
    ): SessionRefreshResult {
        val refreshToken = session.refreshToken
        if (refreshToken.isBlank()) {
            return SessionRefreshResult(
                repositoryResult = AuthRepositoryResult.Failure(
                    code = "INVALID_CREDENTIALS",
                    message = "Refresh token is missing.",
                    kind = AuthFailureKind.UNAUTHORIZED,
                ),
                session = session,
            )
        }
        return applyRefreshResult(
            currentSession = session,
            repositoryResult = authRepository.refreshSession(refreshToken),
        )
    }

    suspend fun refreshSessionWithToken(
        session: AppSessionState,
        refreshToken: String,
    ): SessionRefreshResult {
        return applyRefreshResult(
            currentSession = session,
            repositoryResult = authRepository.refreshSession(refreshToken),
        )
    }

    private fun applyRefreshResult(
        currentSession: AppSessionState,
        repositoryResult: AuthRepositoryResult,
    ): SessionRefreshResult {
        val nextSession = if (repositoryResult is AuthRepositoryResult.Success) {
            createSessionFromAuthSession(
                session = repositoryResult.session,
                fallbackRefreshToken = currentSession.refreshToken,
            )
        } else {
            currentSession
        }
        return SessionRefreshResult(repositoryResult, nextSession)
    }
}

data class SessionRefreshResult(
    val repositoryResult: AuthRepositoryResult,
    val session: AppSessionState,
)

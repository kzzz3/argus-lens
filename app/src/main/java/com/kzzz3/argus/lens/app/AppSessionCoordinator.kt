package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.data.auth.AuthFailureKind
import com.kzzz3.argus.lens.data.auth.AuthRepository
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.data.session.SessionRepository
import com.kzzz3.argus.lens.model.session.AppSessionState

class AppSessionCoordinator(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend fun refreshSession(
        session: AppSessionState,
    ): SessionRefreshResult {
        val currentCredentials = sessionRepository.loadCredentials()
        val refreshToken = currentCredentials.refreshToken
        if (refreshToken.isBlank()) {
            return SessionRefreshResult(
                repositoryResult = AuthRepositoryResult.Failure(
                    code = "INVALID_CREDENTIALS",
                    message = "Refresh token is missing.",
                    kind = AuthFailureKind.UNAUTHORIZED,
                ),
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
        return SessionRefreshResult(repositoryResult, nextSession, nextCredentials)
    }
}

data class SessionRefreshResult(
    val repositoryResult: AuthRepositoryResult,
    val session: AppSessionState,
    val credentials: SessionCredentials,
)

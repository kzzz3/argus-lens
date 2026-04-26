package com.kzzz3.argus.lens.data.session

import com.kzzz3.argus.lens.model.session.AppSessionState

interface SessionRepository {
    suspend fun loadSession(): AppSessionState
    suspend fun loadCredentials(): SessionCredentials
    suspend fun saveSession(
        state: AppSessionState,
        credentials: SessionCredentials = SessionCredentials(),
    )
    suspend fun clearSession()
}

data class SessionCredentials(
    val accessToken: String = "",
    val refreshToken: String = "",
) {
    val hasAccessToken: Boolean
        get() = accessToken.isNotBlank()

    val hasRefreshToken: Boolean
        get() = refreshToken.isNotBlank()
}

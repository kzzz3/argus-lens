package com.kzzz3.argus.lens.data.session

import com.kzzz3.argus.lens.app.session.AppSessionState

interface SessionRepository {
    suspend fun loadSession(): AppSessionState
    suspend fun saveSession(state: AppSessionState)
    suspend fun clearSession()
}

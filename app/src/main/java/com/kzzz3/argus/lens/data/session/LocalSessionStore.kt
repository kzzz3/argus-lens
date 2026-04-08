package com.kzzz3.argus.lens.data.session

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.kzzz3.argus.lens.app.session.AppSessionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LocalSessionStore(
    private val context: Context,
) {
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("argus-lens-session.preferences_pb") }
    )

    suspend fun loadSession(): AppSessionState {
        return dataStore.data.map { preferences ->
            AppSessionState(
                isAuthenticated = preferences[IsAuthenticatedKey] ?: false,
                accountId = preferences[AccountIdKey].orEmpty(),
                displayName = preferences[DisplayNameKey].orEmpty(),
                accessToken = preferences[AccessTokenKey].orEmpty(),
            )
        }.first()
    }

    suspend fun saveSession(
        state: AppSessionState,
    ) {
        dataStore.edit { preferences ->
            preferences[IsAuthenticatedKey] = state.isAuthenticated
            preferences[AccountIdKey] = state.accountId
            preferences[DisplayNameKey] = state.displayName
            preferences[AccessTokenKey] = state.accessToken
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private companion object {
        val IsAuthenticatedKey = booleanPreferencesKey("is_authenticated")
        val AccountIdKey = stringPreferencesKey("account_id")
        val DisplayNameKey = stringPreferencesKey("display_name")
        val AccessTokenKey = stringPreferencesKey("access_token")
    }
}

fun createLocalSessionStore(
    context: Context,
): LocalSessionStore {
    return LocalSessionStore(context.applicationContext)
}

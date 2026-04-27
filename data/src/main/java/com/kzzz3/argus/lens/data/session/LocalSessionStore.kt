package com.kzzz3.argus.lens.data.session

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.kzzz3.argus.lens.model.session.AppSessionState
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val SessionSnapshotFileName = "argus-lens-session-snapshot"
private const val SecureSessionFileName = "argus-lens-secure-session"
private const val KeyStoreAlias = "argus-lens-session-key"
private const val AndroidKeyStoreName = "AndroidKeyStore"
private const val CipherTransformation = "AES/GCM/NoPadding"
private const val SessionDataStoreFileName = "argus-lens-session.preferences_pb"
private const val IsAuthenticatedKeyName = "is_authenticated"
private const val AccountIdKeyName = "account_id"
private const val DisplayNameKeyName = "display_name"
private const val AccessTokenKeyName = "access_token"
private const val RefreshTokenKeyName = "refresh_token"

internal interface LocalSessionStateStore {
    suspend fun loadSession(): AppSessionState
    suspend fun saveSession(state: AppSessionState)
    suspend fun clearSession()
}

internal interface LocalSessionCredentialsStore {
    fun loadCredentials(): SessionCredentials
    fun saveCredentials(credentials: SessionCredentials)
    fun clearCredentials()
}

class LocalSessionStore internal constructor(
    private val sessionStateStore: LocalSessionStateStore,
    private val sessionCredentialsStore: LocalSessionCredentialsStore,
) : SessionRepository {
    constructor(context: Context) : this(
        sessionStateStore = DataStoreLocalSessionStateStore(context.applicationContext),
        sessionCredentialsStore = EncryptedLocalSessionCredentialsStore(context.applicationContext),
    )

    override suspend fun loadSession(): AppSessionState {
        return sessionStateStore.loadSession()
    }

    override suspend fun loadCredentials(): SessionCredentials {
        return sessionCredentialsStore.loadCredentials()
    }

    override suspend fun saveSession(
        state: AppSessionState,
        credentials: SessionCredentials,
    ) {
        sessionStateStore.saveSession(state)
        sessionCredentialsStore.saveCredentials(credentials)
    }

    override suspend fun clearSession() {
        sessionStateStore.clearSession()
        sessionCredentialsStore.clearCredentials()
    }
}

private class DataStoreLocalSessionStateStore(
    context: Context,
) : LocalSessionStateStore {
    private val sessionSnapshotPreferences = context.getSharedPreferences(SessionSnapshotFileName, Context.MODE_PRIVATE)
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(SessionDataStoreFileName) }
    )

    override suspend fun loadSession(): AppSessionState {
        return dataStore.data.map { preferences ->
            AppSessionState(
                isAuthenticated = preferences[IsAuthenticatedKey] ?: false,
                accountId = preferences[AccountIdKey].orEmpty(),
                displayName = preferences[DisplayNameKey].orEmpty(),
            )
        }.first()
    }

    override suspend fun saveSession(state: AppSessionState) {
        dataStore.edit { preferences ->
            preferences[IsAuthenticatedKey] = state.isAuthenticated
            preferences[AccountIdKey] = state.accountId
            preferences[DisplayNameKey] = state.displayName
        }
        saveSessionSnapshot(state)
    }

    override suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
        saveSessionSnapshot(AppSessionState())
    }

    private fun saveSessionSnapshot(state: AppSessionState) {
        sessionSnapshotPreferences.edit()
            .putBoolean(IsAuthenticatedKeyName, state.isAuthenticated)
            .putString(AccountIdKeyName, state.accountId)
            .putString(DisplayNameKeyName, state.displayName)
            .apply()
    }

    private companion object {
        val IsAuthenticatedKey = booleanPreferencesKey(IsAuthenticatedKeyName)
        val AccountIdKey = stringPreferencesKey(AccountIdKeyName)
        val DisplayNameKey = stringPreferencesKey(DisplayNameKeyName)
    }
}

private class EncryptedLocalSessionCredentialsStore(
    private val context: Context,
) : LocalSessionCredentialsStore {
    private val secureTokenPreferences = context.getSharedPreferences(SecureSessionFileName, Context.MODE_PRIVATE)

    override fun loadCredentials(): SessionCredentials {
        return loadSecureCredentials(context, secureTokenPreferences)
    }

    override fun saveCredentials(credentials: SessionCredentials) {
        secureTokenPreferences.edit()
            .putString(AccessTokenKeyName, encryptToken(context, credentials.accessToken))
            .putString(RefreshTokenKeyName, encryptToken(context, credentials.refreshToken))
            .apply()
    }

    override fun clearCredentials() {
        secureTokenPreferences.edit()
            .remove(AccessTokenKeyName)
            .remove(RefreshTokenKeyName)
            .apply()
    }
}

fun createLocalSessionStore(
    context: Context,
): SessionRepository {
    return LocalSessionStore(context.applicationContext)
}

fun createLocalSessionSnapshot(
    context: Context,
): AppSessionState {
    val preferences = context.applicationContext.getSharedPreferences(SessionSnapshotFileName, Context.MODE_PRIVATE)
    return AppSessionState(
        isAuthenticated = preferences.getBoolean(IsAuthenticatedKeyName, false),
        accountId = preferences.getString(AccountIdKeyName, "").orEmpty(),
        displayName = preferences.getString(DisplayNameKeyName, "").orEmpty(),
    )
}

fun createLocalSessionCredentialsSnapshot(
    context: Context,
): SessionCredentials {
    return EncryptedLocalSessionCredentialsStore(context.applicationContext).loadCredentials()
}

private fun loadSecureCredentials(
    context: Context,
    secureTokenPreferences: SharedPreferences,
): SessionCredentials {
    return SessionCredentials(
        accessToken = decryptToken(context, secureTokenPreferences.getString(AccessTokenKeyName, null)),
        refreshToken = decryptToken(context, secureTokenPreferences.getString(RefreshTokenKeyName, null)),
    )
}

private fun encryptToken(context: Context, rawToken: String): String {
    if (rawToken.isBlank()) return ""
    val cipher = Cipher.getInstance(CipherTransformation)
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey(context))
    val encryptedBytes = cipher.doFinal(rawToken.toByteArray(StandardCharsets.UTF_8))
    val payload = cipher.iv + encryptedBytes
    return Base64.encodeToString(payload, Base64.NO_WRAP)
}

private fun decryptToken(context: Context, encodedToken: String?): String {
    if (encodedToken.isNullOrBlank()) return ""
    return try {
        val payload = Base64.decode(encodedToken, Base64.NO_WRAP)
        val iv = payload.copyOfRange(0, 12)
        val encryptedBytes = payload.copyOfRange(12, payload.size)
        val cipher = Cipher.getInstance(CipherTransformation)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(context), GCMParameterSpec(128, iv))
        String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8)
    } catch (_: Exception) {
        ""
    }
}

private fun getOrCreateSecretKey(context: Context): SecretKey {
    val keyStore = KeyStore.getInstance(AndroidKeyStoreName).apply { load(null) }
    val existing = keyStore.getKey(KeyStoreAlias, null) as? SecretKey
    if (existing != null) {
        return existing
    }

    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStoreName)
    val keySpec = KeyGenParameterSpec.Builder(
        KeyStoreAlias,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .build()
    keyGenerator.init(keySpec)
    return keyGenerator.generateKey()
}

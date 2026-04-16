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
import com.kzzz3.argus.lens.app.session.AppSessionState
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
private const val AccessTokenKeyName = "access_token"
private const val RefreshTokenKeyName = "refresh_token"

class LocalSessionStore(
    private val context: Context,
) : SessionRepository {
    private val sessionSnapshotPreferences = context.getSharedPreferences(SessionSnapshotFileName, Context.MODE_PRIVATE)
    private val secureTokenPreferences = context.getSharedPreferences(SecureSessionFileName, Context.MODE_PRIVATE)
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("argus-lens-session.preferences_pb") }
    )

    override suspend fun loadSession(): AppSessionState {
        return dataStore.data.map { preferences ->
            AppSessionState(
                isAuthenticated = preferences[IsAuthenticatedKey] ?: false,
                accountId = preferences[AccountIdKey].orEmpty(),
                displayName = preferences[DisplayNameKey].orEmpty(),
                accessToken = decryptToken(context, secureTokenPreferences.getString(AccessTokenKeyName, null)),
                refreshToken = decryptToken(context, secureTokenPreferences.getString(RefreshTokenKeyName, null)),
            )
        }.first()
    }

    override suspend fun saveSession(
        state: AppSessionState,
    ) {
        dataStore.edit { preferences ->
            preferences[IsAuthenticatedKey] = state.isAuthenticated
            preferences[AccountIdKey] = state.accountId
            preferences[DisplayNameKey] = state.displayName
        }
        saveSessionSnapshot(state)
        saveSecureTokens(state)
    }

    override suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
        saveSessionSnapshot(AppSessionState())
        clearSecureTokens()
    }

    private fun saveSessionSnapshot(state: AppSessionState) {
        sessionSnapshotPreferences.edit()
            .putBoolean(IS_AUTHENTICATED_KEY_NAME, state.isAuthenticated)
            .putString(ACCOUNT_ID_KEY_NAME, state.accountId)
            .putString(DISPLAY_NAME_KEY_NAME, state.displayName)
            .apply()
    }

    private fun saveSecureTokens(state: AppSessionState) {
        secureTokenPreferences.edit()
            .putString(AccessTokenKeyName, encryptToken(context, state.accessToken))
            .putString(RefreshTokenKeyName, encryptToken(context, state.refreshToken))
            .apply()
    }

    private fun clearSecureTokens() {
        secureTokenPreferences.edit()
            .remove(AccessTokenKeyName)
            .remove(RefreshTokenKeyName)
            .apply()
    }

    private companion object {
        const val SESSION_SNAPSHOT_FILE_NAME = "argus-lens-session-snapshot"
        const val IS_AUTHENTICATED_KEY_NAME = "is_authenticated"
        const val ACCOUNT_ID_KEY_NAME = "account_id"
        const val DISPLAY_NAME_KEY_NAME = "display_name"
        val IsAuthenticatedKey = booleanPreferencesKey("is_authenticated")
        val AccountIdKey = stringPreferencesKey("account_id")
        val DisplayNameKey = stringPreferencesKey("display_name")
    }
}

fun createLocalSessionStore(
    context: Context,
) : SessionRepository {
    return LocalSessionStore(context.applicationContext)
}

fun createLocalSessionSnapshot(
    context: Context,
): AppSessionState {
    val preferences = context.applicationContext.getSharedPreferences(SessionSnapshotFileName, Context.MODE_PRIVATE)
    val secureTokenPreferences = context.applicationContext.getSharedPreferences(SecureSessionFileName, Context.MODE_PRIVATE)
    return AppSessionState(
        isAuthenticated = preferences.getBoolean("is_authenticated", false),
        accountId = preferences.getString("account_id", "").orEmpty(),
        displayName = preferences.getString("display_name", "").orEmpty(),
        accessToken = decryptToken(context.applicationContext, secureTokenPreferences.getString(AccessTokenKeyName, null)),
        refreshToken = decryptToken(context.applicationContext, secureTokenPreferences.getString(RefreshTokenKeyName, null)),
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

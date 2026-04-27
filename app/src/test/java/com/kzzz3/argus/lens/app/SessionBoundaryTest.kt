package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.data.auth.AuthSession
import com.kzzz3.argus.lens.model.session.AppSessionState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionBoundaryTest {
    @Test
    fun appSessionState_doesNotDeclareTokenFields() {
        val fieldNames = AppSessionState::class.java.declaredFields.map { it.name }.toSet()

        assertFalse("AppSessionState must not expose access tokens to Parcelable UI state.", "accessToken" in fieldNames)
        assertFalse("AppSessionState must not expose refresh tokens to Parcelable UI state.", "refreshToken" in fieldNames)
    }

    @Test
    fun authSessionMapping_splitsUiIdentityFromCredentials() {
        val authSession = AuthSession(
            accountId = "tester",
            displayName = "Argus Tester",
            accessToken = "access-token",
            refreshToken = "refresh-token",
            message = "ok",
        )

        val uiSession = createSessionFromAuthSession(authSession)
        val credentials = createSessionCredentialsFromAuthSession(authSession)

        assertEquals(AppSessionState(isAuthenticated = true, accountId = "tester", displayName = "Argus Tester"), uiSession)
        assertEquals("access-token", credentials.accessToken)
        assertEquals("refresh-token", credentials.refreshToken)
    }

    @Test
    fun parcelableUiState_doesNotDeclareSessionTokenFields() {
        val tokenFieldPattern = Regex("\\b(val|var)\\s+(accessToken|refreshToken)\\b")
        val violations = listOf("../feature/src/main", "../core/model/src/main")
            .flatMap { root -> File(root).walkTopDown().filter { it.extension == "kt" }.toList() }
            .filter { sourceFile ->
                val source = sourceFile.readText()
                (source.contains("Parcelable") || source.contains("@Parcelize")) && tokenFieldPattern.containsMatchIn(source)
            }
            .map { it.invariantSeparatorsPath }

        assertTrue(
            "Parcelable UI state must not declare accessToken or refreshToken fields: $violations",
            violations.isEmpty(),
        )
    }

    @Test
    fun rememberSaveableState_doesNotPersistSessionTokens() {
        val violations = listOf("src/main", "../feature/src/main", "../core/model/src/main", "../core/ui/src/main")
            .flatMap { root -> File(root).walkTopDown().filter { it.extension == "kt" }.toList() }
            .filter { sourceFile ->
                val source = sourceFile.readText()
                source.contains("rememberSaveable") &&
                    (source.contains("accessToken") || source.contains("refreshToken"))
            }
            .map { it.invariantSeparatorsPath }

        assertTrue(
            "rememberSaveable state must not persist accessToken or refreshToken values: $violations",
            violations.isEmpty(),
        )
    }

    @Test
    fun appRestorableEntryContext_doesNotDeclareSessionTokenFields() {
        val fieldNames = AppRestorableEntryContext::class.java.declaredFields.map { it.name }.toSet()

        assertFalse("Restorable entry context must not expose access tokens to saved state.", "accessToken" in fieldNames)
        assertFalse("Restorable entry context must not expose refresh tokens to saved state.", "refreshToken" in fieldNames)
    }

    @Test
    fun savedStateHandleState_doesNotPersistSessionTokens() {
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensAppViewModel.kt").readText()
        val savedStateSource = viewModelSource
            .lineSequence()
            .filter { line ->
                line.contains("SavedStateHandle") ||
                    line.contains("savedStateHandle") ||
                    line.contains("RESTORABLE_ENTRY")
            }
            .joinToString("\n")

        assertFalse("SavedStateHandle state must not persist access tokens.", savedStateSource.contains("accessToken"))
        assertFalse("SavedStateHandle state must not persist refresh tokens.", savedStateSource.contains("refreshToken"))
    }

    @Test
    fun androidBackupConfiguration_excludesSessionPersistence() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val backupRules = File("src/main/res/xml/backup_rules.xml").readText()
        val dataExtractionRules = File("src/main/res/xml/data_extraction_rules.xml").readText()

        assertTrue(manifest.contains("android:allowBackup=\"false\""))
        assertTrue(manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""))
        assertTrue(manifest.contains("android:fullBackupContent=\"@xml/backup_rules\""))
        assertBackupExcludesSessionPersistence(backupRules)
        assertBackupExcludesSessionPersistence(dataExtractionRules)
    }

    private fun assertBackupExcludesSessionPersistence(source: String) {
        assertTrue(source.contains("<exclude domain=\"sharedpref\" path=\".\""))
        assertTrue(source.contains("<exclude domain=\"database\" path=\".\""))
        assertTrue(source.contains("<exclude domain=\"file\" path=\"datastore/\""))
    }
}

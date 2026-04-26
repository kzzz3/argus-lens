package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.data.auth.AuthSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}

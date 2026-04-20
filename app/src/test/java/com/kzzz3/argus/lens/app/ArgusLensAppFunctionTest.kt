package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.data.auth.AuthSession
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArgusLensAppFunctionTest {

    @Test
    fun incrementCallDurationLabel_incrementsSecondsAndMinutes() {
        assertEquals("00:01", incrementCallDurationLabel("00:00"))
        assertEquals("01:00", incrementCallDurationLabel("00:59"))
    }

    @Test
    fun incrementCallDurationLabel_fallsBackForInvalidInput() {
        assertEquals("00:01", incrementCallDurationLabel("invalid"))
    }

    @Test
    fun isSseAuthFailure_detectsNestedUnauthorizedErrors() {
        val throwable = IllegalStateException(
            "stream failed",
            RuntimeException("HTTP 401 from upstream"),
        )

        assertTrue(isSseAuthFailure(throwable))
    }

    @Test
    fun isSseAuthFailure_ignoresOtherFailures() {
        assertFalse(isSseAuthFailure(IllegalStateException("socket closed")))
    }

    @Test
    fun createSessionFromAuthSession_usesProvidedRefreshToken() {
        val session = createSessionFromAuthSession(
            AuthSession(
                accountId = "tester",
                displayName = "Argus Tester",
                accessToken = "access-token",
                refreshToken = "refresh-token",
                message = "ok",
            ),
        )

        assertTrue(session.isAuthenticated)
        assertEquals("tester", session.accountId)
        assertEquals("Argus Tester", session.displayName)
        assertEquals("access-token", session.accessToken)
        assertEquals("refresh-token", session.refreshToken)
    }

    @Test
    fun createSessionFromAuthSession_keepsFallbackRefreshTokenWhenResponseIsBlank() {
        val session = createSessionFromAuthSession(
            AuthSession(
                accountId = "tester",
                displayName = "Argus Tester",
                accessToken = "access-token",
                refreshToken = "",
                message = "ok",
            ),
            fallbackRefreshToken = "persisted-refresh",
        )

        assertEquals("persisted-refresh", session.refreshToken)
    }

    @Test
    fun resolvePreviewDisplayName_returnsDefaultWhenBlank() {
        assertEquals(DEFAULT_PREVIEW_DISPLAY_NAME, resolvePreviewDisplayName(""))
        assertEquals("Li Si", resolvePreviewDisplayName("Li Si"))
    }

    @Test
    fun createPostAuthUiState_resetsWalletAndReconnectsForLogin() {
        val uiState = createPostAuthUiState(
            signedInState = AppSignedInState(
                conversationThreadsState = sampleConversationThreadsState(),
                hydratedConversationAccountId = "tester",
                callSessionState = com.kzzz3.argus.lens.feature.call.CallSessionState(),
                selectedConversationId = "",
            ),
            accountId = "tester",
        )

        assertEquals(1, uiState.realtimeReconnectIncrement)
        assertEquals("tester", uiState.nextAuthFormState.account)
        assertEquals(null, uiState.nextAuthFormState.submitResult)
        assertEquals(false, uiState.nextAuthFormState.isSubmitting)
        assertEquals("tester", uiState.hydratedConversationAccountId)
    }

    @Test
    fun completeRegistrationForm_stopsSubmittingAndKeepsAccount() {
        val state = completeRegistrationForm(
            formState = RegisterFormState(isSubmitting = true),
            submitResult = "Created",
        )

        assertFalse(state.isSubmitting)
        assertEquals("Created", state.submitResult)
    }

    @Test
    fun createContactsStatusUpdate_marksSuccessMessage() {
        val state = createContactsStatusUpdate(
            currentState = ContactsState(),
            message = "Friend request sent.",
            isError = false,
        )

        assertEquals("Friend request sent.", state.statusMessage)
        assertFalse(state.isStatusError)
    }

    @Test
    fun createFriendRequestStatusState_updatesSnapshotAndClearsError() {
        val snapshot = FriendRequestsSnapshot(emptyList(), emptyList())
        val state = createFriendRequestStatusState(
            snapshot = snapshot,
            message = "Friend request accepted.",
            isError = false,
        )

        assertEquals(snapshot, state.snapshot)
        assertEquals("Friend request accepted.", state.message)
        assertFalse(state.isError)
    }

    private fun sampleConversationThreadsState() =
        com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState()
}

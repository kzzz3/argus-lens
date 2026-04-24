package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.contacts.createContactsStatusUpdate
import com.kzzz3.argus.lens.feature.contacts.createFriendRequestStatusState
import com.kzzz3.argus.lens.feature.contacts.resolveDirectConversationTarget
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.realtime.isSseAuthFailure
import com.kzzz3.argus.lens.feature.wallet.WalletTransferDirection
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.feature.wallet.counterpartyDisplayName
import com.kzzz3.argus.lens.feature.wallet.resolveWalletTransferMetadata
import com.kzzz3.argus.lens.feature.wallet.toDirection
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.data.auth.AuthSession
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.data.payment.PaymentHistoryEntry
import com.kzzz3.argus.lens.data.payment.PaymentReceipt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArgusLensAppFunctionTest {


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

    @Test
    fun resolveDirectConversationTarget_prefersExistingThread() {
        val target = resolveDirectConversationTarget(
            currentAccountId = "tester",
            requestedConversationId = "conv-existing",
            friends = listOf(FriendEntry("lisi", "Li Si", "friend")),
            existingThreadIds = setOf("conv-existing"),
        )

        assertEquals("conv-existing", target.conversationId)
        assertEquals(false, target.requiresRefresh)
        assertEquals(false, target.requiresPlaceholder)
    }

    @Test
    fun resolveDirectConversationTarget_usesFriendConversationIdWhenKnownFriendMatches() {
        val target = resolveDirectConversationTarget(
            currentAccountId = "tester",
            requestedConversationId = "lisi",
            friends = listOf(FriendEntry("lisi", "Li Si", "friend")),
            existingThreadIds = emptySet(),
        )

        assertTrue(target.conversationId.contains("lisi"))
        assertEquals(true, target.requiresRefresh)
        assertEquals(false, target.requiresPlaceholder)
        assertEquals("Li Si", target.placeholderTitle)
    }

    @Test
    fun resolveDirectConversationTarget_fallsBackToPlaceholderForUnknownConversation() {
        val target = resolveDirectConversationTarget(
            currentAccountId = "tester",
            requestedConversationId = "unknown-id",
            friends = emptyList(),
            existingThreadIds = emptySet(),
        )

        assertEquals("unknown-id", target.conversationId)
        assertEquals(false, target.requiresRefresh)
        assertEquals(true, target.requiresPlaceholder)
        assertEquals("unknown-id", target.placeholderTitle)
    }

    @Test
    fun applyWalletRequestResult_updatesStateWhenRequestIsActive() {
        val updatedState = applyWalletRequestResult(
            currentState = WalletState(),
            isActive = true,
        ) {
            it.copy(statusMessage = "Updated")
        }

        assertEquals("Updated", updatedState.statusMessage)
    }

    @Test
    fun applyWalletRequestResult_keepsStateWhenRequestIsStale() {
        val currentState = WalletState(statusMessage = "Current")
        val updatedState = applyWalletRequestResult(
            currentState = currentState,
            isActive = false,
        ) {
            it.copy(statusMessage = "Updated")
        }

        assertEquals("Current", updatedState.statusMessage)
    }

    @Test
    fun resolveWalletTransferMetadata_returnsSentForPayer() {
        val metadata = resolveWalletTransferMetadata(
            currentAccountId = "tester",
            payerAccountId = "tester",
            payerDisplayName = "Tester",
            recipientDisplayName = "Li Si",
        )

        assertEquals(WalletTransferDirection.Sent, metadata.direction)
        assertEquals("Li Si", metadata.counterpartyDisplayName)
    }

    @Test
    fun resolveWalletTransferMetadata_returnsReceivedForRecipient() {
        val metadata = resolveWalletTransferMetadata(
            currentAccountId = "tester",
            payerAccountId = "wangwu",
            payerDisplayName = "Wang Wu",
            recipientDisplayName = "Tester",
        )

        assertEquals(WalletTransferDirection.Received, metadata.direction)
        assertEquals("Wang Wu", metadata.counterpartyDisplayName)
    }

    @Test
    fun paymentReceiptAndHistoryEntry_shareWalletTransferResolution() {
        val receipt = PaymentReceipt(
            paymentId = "payment-1",
            scanSessionId = "scan-1",
            status = "SUCCESS",
            payerAccountId = "tester",
            payerDisplayName = "Tester",
            payerBalanceAfter = 100.0,
            recipientAccountId = "lisi",
            recipientDisplayName = "Li Si",
            recipientBalanceAfter = 150.0,
            amount = 50.0,
            currency = "CNY",
            note = "Lunch",
            paidAt = "2026-04-20T12:00:00Z",
        )
        val history = PaymentHistoryEntry(
            paymentId = "payment-1",
            payerAccountId = "tester",
            payerDisplayName = "Tester",
            recipientAccountId = "lisi",
            recipientDisplayName = "Li Si",
            amount = 50.0,
            currency = "CNY",
            status = "SUCCESS",
            paidAt = "2026-04-20T12:00:00Z",
        )

        assertEquals(WalletTransferDirection.Sent, receipt.toDirection("tester"))
        assertEquals(WalletTransferDirection.Sent, history.toDirection("tester"))
        assertEquals("Li Si", history.counterpartyDisplayName("tester"))
    }


    private fun sampleConversationThreadsState() =
        com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState()
}



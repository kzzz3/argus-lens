package com.kzzz3.argus.lens.feature.me

import com.kzzz3.argus.lens.core.data.friend.FriendEntry
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.feature.wallet.WalletSummaryUi
import com.kzzz3.argus.lens.model.session.AppSessionState
import org.junit.Assert.assertEquals
import org.junit.Test

class MeStateFactoryTest {

    @Test
    fun createMeUiState_buildsProfileWalletAndStatCards() {
        val state = createMeUiState(
            sessionState = AppSessionState(
                isAuthenticated = true,
                accountId = "tester",
                displayName = "Argus Tester",
            ),
            walletState = WalletState(
                summary = WalletSummaryUi(
                    accountId = "tester",
                    displayName = "Argus Tester",
                    balance = "1000.00",
                    currency = "CNY",
                )
            ),
            friends = listOf(
                FriendEntry("lisi", "Li Si", "friend"),
                FriendEntry("wangwu", "Wang Wu", "friend"),
            ),
            conversationThreads = listOf(
                InboxConversationThread("conv-1", "Li Si", "Direct friend conversation", 0, emptyList()),
                InboxConversationThread("conv-2", "Wang Wu", "Direct friend conversation", 0, emptyList()),
                InboxConversationThread("conv-3", "Team", "Group conversation", 0, emptyList()),
            ),
            shellStatusLabel = "Online",
            shellStatusSummary = "Realtime channel connected and syncing now.",
        )

        assertEquals("Argus Tester", state.displayName)
        assertEquals("tester", state.accountId)
        assertEquals("Wallet balance · CNY 1000.00", state.walletSummary)
        assertEquals("Online", state.statusLabel)
        assertEquals("Realtime channel connected and syncing now.", state.summaryLine)
        assertEquals("3 active threads", state.cards.first { it.title == "Chats" }.value)
        assertEquals("2 saved friends", state.cards.first { it.title == "Contacts" }.value)
    }

    @Test
    fun createMeUiState_usesFallbackIdentityAndWalletTextWhenSessionIsBlank() {
        val state = createMeUiState(
            sessionState = AppSessionState(),
            walletState = WalletState(),
            friends = emptyList(),
            conversationThreads = emptyList(),
            shellStatusLabel = "Signed out",
            shellStatusSummary = "Sign in to enter the Argus IM shell.",
        )

        assertEquals("Argus User", state.displayName)
        assertEquals("offline-preview", state.accountId)
        assertEquals("Wallet balance · Not synced yet", state.walletSummary)
        assertEquals("0 active threads", state.cards.first { it.title == "Chats" }.value)
        assertEquals("0 saved friends", state.cards.first { it.title == "Contacts" }.value)
    }
}

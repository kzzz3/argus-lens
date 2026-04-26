package com.kzzz3.argus.lens.feature.me

import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.model.session.AppSessionState

fun createMeUiState(
    sessionState: AppSessionState,
    walletState: WalletState,
    friends: List<FriendEntry>,
    conversationThreads: List<InboxConversationThread>,
    shellStatusLabel: String,
    shellStatusSummary: String,
): MeUiState {
    return MeUiState(
        displayName = sessionState.displayName.ifBlank { "Argus User" },
        accountId = sessionState.accountId.ifBlank { "offline-preview" },
        walletSummary = walletState.summary?.let { summary ->
            "Wallet balance · ${summary.currency} ${summary.balance}"
        } ?: "Wallet balance · Not synced yet",
        statusLabel = shellStatusLabel,
        summaryLine = shellStatusSummary,
        cards = listOf(
            MeStatCardUi(
                title = "Chats",
                value = "${conversationThreads.size} active threads",
                supporting = "Open the shell instantly from cache, then let realtime catch up in the background.",
            ),
            MeStatCardUi(
                title = "Contacts",
                value = "${friends.size} saved friends",
                supporting = "Friend links stay ready so you can jump into direct conversations without hunting through menus.",
            ),
        ),
    )
}

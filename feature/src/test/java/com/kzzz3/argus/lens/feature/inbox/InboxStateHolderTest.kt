package com.kzzz3.argus.lens.feature.inbox

import com.kzzz3.argus.lens.model.session.createAuthenticatedSession
import org.junit.Assert.assertEquals
import org.junit.Test

class InboxStateHolderTest {
    @Test
    fun replaceInputs_recomputesInboxUiStateFromSessionRealtimeShellAndThreads() {
        val holder = InboxStateHolder()
        val firstThreads = ConversationThreadsState(
            threads = listOf(conversationThread(id = "conversation-1", title = "Alice")),
        )

        holder.replaceInputs(
            sessionState = createAuthenticatedSession(
                accountId = "argus_tester",
                displayName = "Argus Tester",
            ),
            threadsState = firstThreads,
            realtimeStatusLabel = "live",
            shellStatusLabel = "Online",
        )

        assertEquals("Online", holder.state.value.uiState.shellStatusLabel)
        assertEquals("Signed in as Argus Tester", holder.state.value.uiState.sessionLabel)
        assertEquals("Alice", holder.state.value.uiState.conversations.single().title)

        holder.replaceInputs(
            sessionState = createAuthenticatedSession(
                accountId = "argus_tester",
                displayName = "Argus Tester",
            ),
            threadsState = firstThreads.copy(
                threads = listOf(conversationThread(id = "conversation-2", title = "Bob")),
            ),
            realtimeStatusLabel = "recovering",
            shellStatusLabel = "Reconnecting",
        )

        assertEquals("Reconnecting", holder.state.value.uiState.shellStatusLabel)
        assertEquals(
            "Account ID: argus_tester. Shell: Reconnecting · Realtime: recovering.",
            holder.state.value.uiState.sessionSummary,
        )
        assertEquals("Bob", holder.state.value.uiState.conversations.single().title)
    }

    @Test
    fun handleAction_openConversationDelegatesWithoutOwningThreadMutation() {
        val holder = InboxStateHolder()
        val initialState = holder.state.value
        val events = mutableListOf<String>()

        holder.handleAction(
            action = InboxAction.OpenConversation("conversation-1"),
            callbacks = inboxCallbacks(
                onOpenConversation = { events += "conversation:$it" },
                onOpenContacts = { events += "contacts" },
                onOpenWallet = { events += "wallet" },
                onSignOutToHud = { events += "sign-out" },
            ),
        )

        assertEquals(initialState, holder.state.value)
        assertEquals(listOf("conversation:conversation-1"), events)
    }

    @Test
    fun handleAction_topLevelAndSignOutActionsEmitRouteAgnosticCallbacks() {
        val holder = InboxStateHolder()
        val events = mutableListOf<String>()
        val callbacks = inboxCallbacks(
            onOpenConversation = { events += "conversation:$it" },
            onOpenContacts = { events += "contacts" },
            onOpenWallet = { events += "wallet" },
            onSignOutToHud = { events += "sign-out" },
        )

        holder.handleAction(InboxAction.OpenContacts, callbacks)
        holder.handleAction(InboxAction.OpenWallet, callbacks)
        holder.handleAction(InboxAction.SignOutToHud, callbacks)

        assertEquals(listOf("contacts", "wallet", "sign-out"), events)
    }

    private fun inboxCallbacks(
        onOpenConversation: (String) -> Unit = {},
        onOpenContacts: () -> Unit = {},
        onOpenWallet: () -> Unit = {},
        onSignOutToHud: () -> Unit = {},
    ): InboxStateHolderCallbacks {
        return InboxStateHolderCallbacks(
            onOpenConversation = onOpenConversation,
            onOpenContacts = onOpenContacts,
            onOpenWallet = onOpenWallet,
            onSignOutToHud = onSignOutToHud,
        )
    }

    private fun conversationThread(
        id: String,
        title: String,
    ): InboxConversationThread {
        return InboxConversationThread(
            id = id,
            title = title,
            subtitle = "direct",
            unreadCount = 0,
            messages = listOf(
                ChatMessageItem(
                    id = "$id-message",
                    senderDisplayName = title,
                    body = "hello",
                    timestampLabel = "09:45",
                    isFromCurrentUser = false,
                    deliveryStatus = ChatMessageDeliveryStatus.Delivered,
                ),
            ),
        )
    }
}

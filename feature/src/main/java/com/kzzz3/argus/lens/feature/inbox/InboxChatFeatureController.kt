package com.kzzz3.argus.lens.feature.inbox

import com.kzzz3.argus.lens.feature.call.CallSessionState

data class InboxChatFeatureSnapshot(
    val threadsState: ConversationThreadsState,
    val chatState: ChatState?,
)

data class InboxChatFeatureCallbacks(
    val onOpenContacts: () -> Unit,
    val onOpenWallet: () -> Unit,
    val onSignOutToHud: () -> Unit,
    val onConversationThreadsChanged: (ConversationThreadsState) -> Unit,
    val onConversationOpened: (String) -> Unit,
    val onNavigateBackToInbox: () -> Unit,
    val onChatStatusChanged: (String?, Boolean) -> Unit,
    val onCallSessionStateChanged: (CallSessionState) -> Unit,
    val onNavigateToCallSession: () -> Unit,
    val isCallSessionRouteActive: () -> Boolean,
)

class InboxChatFeatureController(
    private val inboxStateHolder: InboxStateHolder,
    private val inboxRouteHandler: InboxRouteHandler,
    private val chatRouteHandler: ChatRouteHandler,
) {
    fun handleInboxAction(
        action: InboxAction,
        snapshot: InboxChatFeatureSnapshot,
        callbacks: InboxChatFeatureCallbacks,
    ) {
        inboxStateHolder.handleAction(
            action = action,
            callbacks = InboxStateHolderCallbacks(
                onOpenConversation = { conversationId -> openConversation(conversationId, snapshot, callbacks) },
                onOpenContacts = callbacks.onOpenContacts,
                onOpenWallet = callbacks.onOpenWallet,
                onSignOutToHud = callbacks.onSignOutToHud,
            ),
        )
    }

    fun handleChatAction(
        action: ChatAction,
        snapshot: InboxChatFeatureSnapshot,
        callbacks: InboxChatFeatureCallbacks,
    ) {
        val chatState = snapshot.chatState ?: return
        chatRouteHandler.handleAction(
            action = action,
            request = ChatRouteRequest(
                threadsState = snapshot.threadsState,
                chatState = chatState,
            ),
            callbacks = ChatRouteCallbacks(
                onNavigateBackToInbox = callbacks.onNavigateBackToInbox,
                onConversationThreadsChanged = callbacks.onConversationThreadsChanged,
                onChatStatusChanged = callbacks.onChatStatusChanged,
                onCallSessionStateChanged = callbacks.onCallSessionStateChanged,
                onNavigateToCallSession = callbacks.onNavigateToCallSession,
                isCallSessionRouteActive = callbacks.isCallSessionRouteActive,
            ),
        )
    }

    private fun openConversation(
        conversationId: String,
        snapshot: InboxChatFeatureSnapshot,
        callbacks: InboxChatFeatureCallbacks,
    ) {
        inboxRouteHandler.openConversation(
            conversationId = conversationId,
            request = InboxRouteRequest(threadsState = snapshot.threadsState),
            callbacks = InboxRouteCallbacks(
                onConversationThreadsChanged = callbacks.onConversationThreadsChanged,
                onConversationOpened = callbacks.onConversationOpened,
            ),
        )
    }
}

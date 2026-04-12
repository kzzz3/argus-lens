package com.kzzz3.argus.lens.feature.inbox

import com.kzzz3.argus.lens.app.session.AppSessionState

fun createInboxUiState(
    sessionState: AppSessionState,
    threads: List<InboxConversationThread>,
): InboxUiState {
    return InboxUiState(
        title = "Stage-1 Inbox",
        subtitle = "This module now renders a real local conversation list instead of the old placeholder.",
        sessionLabel = if (sessionState.isAuthenticated) {
            "Signed in as ${sessionState.displayName}"
        } else {
            "No active session"
        },
        sessionSummary = if (sessionState.isAuthenticated) {
            "Account ID: ${sessionState.accountId}. Auth is real; messages are local for this step."
        } else {
            "Session is empty."
        },
        conversations = threads.map { thread ->
            val latestMessage = thread.messages.lastOrNull()
            val statusLabel = when (latestMessage?.deliveryStatus) {
                ChatMessageDeliveryStatus.Sending -> "Sending"
                ChatMessageDeliveryStatus.Sent -> if (latestMessage.isFromCurrentUser) "Sent" else null
                ChatMessageDeliveryStatus.Delivered -> if (latestMessage.isFromCurrentUser) "Delivered" else null
                ChatMessageDeliveryStatus.Read -> if (latestMessage.isFromCurrentUser) "Read" else null
                ChatMessageDeliveryStatus.Failed -> "Failed"
                ChatMessageDeliveryStatus.Recalled -> "Recalled"
                null -> null
            }
            val statusColorToken = when (latestMessage?.deliveryStatus) {
                ChatMessageDeliveryStatus.Failed -> InboxStatusColorToken.Warning
                ChatMessageDeliveryStatus.Sent,
                ChatMessageDeliveryStatus.Delivered,
                ChatMessageDeliveryStatus.Read -> if (latestMessage.isFromCurrentUser) {
                    InboxStatusColorToken.Success
                } else {
                    InboxStatusColorToken.Neutral
                }
                ChatMessageDeliveryStatus.Recalled -> InboxStatusColorToken.Neutral
                else -> InboxStatusColorToken.Neutral
            }
            InboxConversationItem(
                id = thread.id,
                title = thread.title,
                subtitle = thread.subtitle,
                preview = latestMessage?.body ?: "No messages yet",
                timestampLabel = latestMessage?.timestampLabel ?: "--:--",
                unreadCount = thread.unreadCount,
                latestMessageStatusLabel = statusLabel,
                latestMessageStatusColorToken = statusColorToken,
            )
        },
        contactsActionLabel = "Open contacts",
        primaryActionLabel = "Sign out to HUD",
    )
}

package com.kzzz3.argus.lens.feature.inbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatStateHolderTest {
    @Test
    fun replaceInputs_recomputesChatStateAndUiStateFromActiveChatConversationStatusAndSessionDisplayName() {
        val holder = ChatStateHolder()
        val threads = ConversationThreadsState(
            threads = listOf(
                conversationThread(
                    id = "conversation-1",
                    title = "Alice",
                    subtitle = "Online",
                    draftMessage = "Draft reply",
                ),
                conversationThread(
                    id = "conversation-2",
                    title = "Bob",
                    subtitle = "Away",
                ),
            ),
        )

        holder.replaceInputs(
            currentUserDisplayName = "Argus Tester",
            threadsState = threads,
            activeChatConversationId = "conversation-1",
            statusMessage = "Uploaded",
            isStatusError = false,
        )

        assertEquals("conversation-1", holder.state.value.chatState?.conversationId)
        assertEquals("Alice", holder.state.value.chatState?.conversationTitle)
        assertEquals("Argus Tester", holder.state.value.chatState?.currentUserDisplayName)
        assertEquals("Draft reply", holder.state.value.chatState?.draftMessage)
        assertEquals("Alice", holder.state.value.uiState?.conversationTitle)
        assertEquals("Uploaded", holder.state.value.uiState?.statusMessage)
        assertFalse(holder.state.value.uiState?.isStatusError ?: true)

        holder.replaceInputs(
            currentUserDisplayName = "Argus Tester",
            threadsState = threads,
            activeChatConversationId = "conversation-2",
            statusMessage = "Upload failed",
            isStatusError = true,
        )

        assertEquals("conversation-2", holder.state.value.chatState?.conversationId)
        assertEquals("Bob", holder.state.value.uiState?.conversationTitle)
        assertEquals("Upload failed", holder.state.value.uiState?.statusMessage)
        assertTrue(holder.state.value.uiState?.isStatusError ?: false)
    }

    @Test
    fun replaceInputs_missingActiveChatConversationClearsChatStateAndUiState() {
        val holder = ChatStateHolder()
        holder.replaceInputs(
            currentUserDisplayName = "Argus Tester",
            threadsState = ConversationThreadsState(
                threads = listOf(conversationThread(id = "conversation-1", title = "Alice")),
            ),
            activeChatConversationId = "conversation-1",
            statusMessage = null,
            isStatusError = false,
        )

        holder.replaceInputs(
            currentUserDisplayName = "Argus Tester",
            threadsState = ConversationThreadsState(
                threads = listOf(conversationThread(id = "conversation-2", title = "Bob")),
            ),
            activeChatConversationId = "conversation-1",
            statusMessage = "Stale status",
            isStatusError = true,
        )

        assertNull(holder.state.value.chatState)
        assertNull(holder.state.value.uiState)
    }

    private fun conversationThread(
        id: String,
        title: String,
        subtitle: String = "direct",
        draftMessage: String = "",
    ): InboxConversationThread {
        return InboxConversationThread(
            id = id,
            title = title,
            subtitle = subtitle,
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
            draftMessage = draftMessage,
        )
    }
}

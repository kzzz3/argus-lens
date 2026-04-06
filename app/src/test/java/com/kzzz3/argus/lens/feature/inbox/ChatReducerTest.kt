package com.kzzz3.argus.lens.feature.inbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatReducerTest {

    @Test
    fun sendMessage_withDraft_appendsLocalMessageAndClearsDraft() {
        val state = ChatState(
            conversationId = "conv-1",
            conversationTitle = "Zhang San",
            conversationSubtitle = "1:1 direct chat",
            currentUserDisplayName = "Argus Tester",
            messages = listOf(
                ChatMessageItem(
                    id = "m1",
                    senderDisplayName = "Zhang San",
                    body = "Hello",
                    timestampLabel = "09:24",
                    isFromCurrentUser = false,
                )
            ),
            draftMessage = "Ship the next Android module today.",
        )

        val result = reduceChatState(state, ChatAction.SendMessage)

        assertEquals("", result.state.draftMessage)
        assertEquals(2, result.state.messages.size)
        assertEquals("Ship the next Android module today.", result.state.messages.last().body)
        assertTrue(result.state.messages.last().isFromCurrentUser)
        assertEquals(null, result.effect)
    }

    @Test
    fun sendMessage_withBlankDraft_keepsStateUnchanged() {
        val state = ChatState(
            conversationId = "conv-1",
            conversationTitle = "Zhang San",
            conversationSubtitle = "1:1 direct chat",
            currentUserDisplayName = "Argus Tester",
            messages = emptyList(),
            draftMessage = "   ",
        )

        val result = reduceChatState(state, ChatAction.SendMessage)

        assertEquals(state, result.state)
        assertEquals(null, result.effect)
    }

    @Test
    fun navigateBack_requestsInboxNavigation() {
        val state = ChatState(
            conversationId = "conv-1",
            conversationTitle = "Zhang San",
            conversationSubtitle = "1:1 direct chat",
            currentUserDisplayName = "Argus Tester",
            messages = emptyList(),
        )

        val result = reduceChatState(state, ChatAction.NavigateBackToInbox)

        assertEquals(ChatEffect.NavigateBackToInbox, result.effect)
    }
}

package com.kzzz3.argus.lens.feature.inbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ChatCoordinatorTest {
    @Test
    fun summarizeOutgoingDispatch_usesLatestStateAndFirstFailureMessage() {
        val firstState = ConversationThreadsState(
            threads = listOf(
                InboxConversationThread(
                    id = "conv-1",
                    title = "Chat",
                    subtitle = "",
                    unreadCount = 0,
                    messages = listOf(
                        ChatMessageItem(
                            id = "message-1",
                            senderDisplayName = "Tester",
                            body = "hello",
                            timestampLabel = "now",
                            deliveryStatus = ChatMessageDeliveryStatus.Sent,
                            isFromCurrentUser = true,
                        )
                    ),
                )
            )
        )
        val secondState = firstState.copy()

        val summary = summarizeOutgoingDispatch(
            results = listOf(
                OutgoingDispatchResult(state = firstState, failureMessage = "Upload failed"),
                OutgoingDispatchResult(state = secondState, failureMessage = "Ignored later failure"),
            )
        )

        assertNotNull(summary)
        assertEquals(secondState, summary!!.state)
        assertEquals("Upload failed", summary.failureMessage)
    }

    @Test
    fun summarizeOutgoingDispatch_handlesEmptyBatch() {
        assertEquals(null, summarizeOutgoingDispatch(emptyList()))
    }
}

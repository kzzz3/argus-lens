package com.kzzz3.argus.lens.feature.inbox

import com.kzzz3.argus.lens.model.session.createAuthenticatedSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InboxStateFactoryTest {

    @Test
    fun createInboxUiState_outgoingDeliveredMessageShowsDeliveredStatus() {
        val uiState = createInboxUiState(
            sessionState = createAuthenticatedSession(
                accountId = "argus_tester",
                displayName = "Argus Tester",
            ),
            realtimeStatusLabel = "live",
            shellStatusLabel = "Online",
            threads = listOf(
                InboxConversationThread(
                    id = "conv-1",
                    title = "Alice",
                    subtitle = "direct",
                    unreadCount = 0,
                    messages = listOf(
                        ChatMessageItem(
                            id = "m-1",
                            senderDisplayName = "Argus Tester",
                            body = "Delivered already",
                            timestampLabel = "09:45",
                            isFromCurrentUser = true,
                            deliveryStatus = ChatMessageDeliveryStatus.Delivered,
                        )
                    ),
                )
            ),
        )

        val item = uiState.conversations.single()
        assertEquals("Delivered", item.latestMessageStatusLabel)
        assertEquals(InboxStatusColorToken.Success, item.latestMessageStatusColorToken)
    }

    @Test
    fun createInboxUiState_incomingReadMessageDoesNotShowOutboundReceiptStatus() {
        val uiState = createInboxUiState(
            sessionState = createAuthenticatedSession(
                accountId = "argus_tester",
                displayName = "Argus Tester",
            ),
            realtimeStatusLabel = "live",
            shellStatusLabel = "Online",
            threads = listOf(
                InboxConversationThread(
                    id = "conv-2",
                    title = "Bob",
                    subtitle = "direct",
                    unreadCount = 1,
                    messages = listOf(
                        ChatMessageItem(
                            id = "m-2",
                            senderDisplayName = "Bob",
                            body = "Seen by me, not an outbound receipt",
                            timestampLabel = "09:46",
                            isFromCurrentUser = false,
                            deliveryStatus = ChatMessageDeliveryStatus.Read,
                        )
                    ),
                )
            ),
        )

        val item = uiState.conversations.single()
        assertNull(item.latestMessageStatusLabel)
        assertEquals(InboxStatusColorToken.Neutral, item.latestMessageStatusColorToken)
    }

    @Test
    fun createInboxUiState_includesRealtimeStatusInSessionSummary() {
        val uiState = createInboxUiState(
            sessionState = createAuthenticatedSession(
                accountId = "argus_tester",
                displayName = "Argus Tester",
            ),
            threads = emptyList(),
            realtimeStatusLabel = "recovering",
            shellStatusLabel = "Offline",
        )

        assertEquals("Account ID: argus_tester. Shell: Offline · Realtime: recovering.", uiState.sessionSummary)
    }

    @Test
    fun createInboxUiState_attachmentMessageUsesFileNamePreview() {
        val uiState = createInboxUiState(
            sessionState = createAuthenticatedSession(
                accountId = "argus_tester",
                displayName = "Argus Tester",
            ),
            realtimeStatusLabel = "live",
            shellStatusLabel = "Online",
            threads = listOf(
                InboxConversationThread(
                    id = "conv-file",
                    title = "Files",
                    subtitle = "direct",
                    unreadCount = 0,
                    messages = listOf(
                        ChatMessageItem(
                            id = "m-file",
                            senderDisplayName = "Argus Tester",
                            body = "design-spec.png",
                            timestampLabel = "10:10",
                            isFromCurrentUser = true,
                            deliveryStatus = ChatMessageDeliveryStatus.Delivered,
                            attachment = ChatMessageAttachment(
                                attachmentId = "att-1",
                                attachmentType = "IMAGE",
                                fileName = "design-spec.png",
                                contentType = "image/png",
                                contentLength = 11,
                            ),
                        )
                    ),
                )
            ),
        )

        val item = uiState.conversations.single()
        assertEquals("design-spec.png", item.preview)
    }

    @Test
    fun createInboxUiState_failedMessageShowsWarningStatus() {
        val uiState = createInboxUiState(
            sessionState = createAuthenticatedSession(
                accountId = "argus_tester",
                displayName = "Argus Tester",
            ),
            realtimeStatusLabel = "live",
            shellStatusLabel = "Online",
            threads = listOf(
                InboxConversationThread(
                    id = "conv-3",
                    title = "Charlie",
                    subtitle = "direct",
                    unreadCount = 0,
                    messages = listOf(
                        ChatMessageItem(
                            id = "m-3",
                            senderDisplayName = "Argus Tester",
                            body = "Failed message",
                            timestampLabel = "09:47",
                            isFromCurrentUser = true,
                            deliveryStatus = ChatMessageDeliveryStatus.Failed,
                        )
                    ),
                )
            ),
        )

        val item = uiState.conversations.single()
        assertEquals("Failed", item.latestMessageStatusLabel)
        assertEquals(InboxStatusColorToken.Warning, item.latestMessageStatusColorToken)
    }
}

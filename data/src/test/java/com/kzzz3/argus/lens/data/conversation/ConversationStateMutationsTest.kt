package com.kzzz3.argus.lens.data.conversation

import com.kzzz3.argus.lens.model.conversation.ChatDraftAttachment
import com.kzzz3.argus.lens.model.conversation.ChatDraftAttachmentKind
import com.kzzz3.argus.lens.model.conversation.ChatMessageDeliveryStatus
import com.kzzz3.argus.lens.model.conversation.ChatMessageItem
import com.kzzz3.argus.lens.model.conversation.ConversationThreadsState
import com.kzzz3.argus.lens.model.conversation.InboxConversationThread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationStateMutationsTest {

    @Test
    fun mergeRemoteConversationSummaries_preservesLocalHistoryAndDrafts() {
        val localState = ConversationThreadsState(
            threads = listOf(
                InboxConversationThread(
                    id = "conv-1",
                    title = "Alice local",
                    subtitle = "stale",
                    unreadCount = 1,
                    syncCursor = "cursor-local",
                    messages = listOf(
                        ChatMessageItem(
                            id = "m-1",
                            senderDisplayName = "Alice",
                            body = "Full local history",
                            timestampLabel = "09:00",
                            isFromCurrentUser = false,
                            deliveryStatus = ChatMessageDeliveryStatus.Delivered,
                        )
                    ),
                    draftMessage = "draft text",
                    draftAttachments = listOf(
                        ChatDraftAttachment(
                            id = "draft-1",
                            kind = ChatDraftAttachmentKind.Image,
                            title = "Image draft 1",
                            summary = "draft summary",
                        )
                    ),
                ),
                InboxConversationThread(
                    id = "conv-local-only",
                    title = "Local only",
                    subtitle = "offline",
                    unreadCount = 0,
                    messages = emptyList(),
                )
            )
        )

        val result = mergeRemoteConversationSummaries(
            localState = localState,
            remoteSummaries = listOf(
                RemoteConversationSummary(
                    id = "conv-1",
                    title = "Alice",
                    subtitle = "remote summary",
                    preview = "remote preview",
                    timestampLabel = "10:00",
                    unreadCount = 3,
                    syncCursor = "cursor-remote",
                ),
                RemoteConversationSummary(
                    id = "conv-2",
                    title = "Bob",
                    subtitle = "new remote",
                    preview = "hello from remote",
                    timestampLabel = "11:00",
                    unreadCount = 2,
                    syncCursor = "cursor-2",
                ),
            ),
        )

        assertEquals(listOf("conv-1", "conv-2", "conv-local-only"), result.threads.map { it.id })
        assertEquals("Alice", result.threads[0].title)
        assertEquals("remote summary", result.threads[0].subtitle)
        assertEquals(3, result.threads[0].unreadCount)
        assertEquals("cursor-remote", result.threads[0].syncCursor)
        assertEquals("draft text", result.threads[0].draftMessage)
        assertEquals(1, result.threads[0].draftAttachments.size)
        assertEquals("m-1", result.threads[0].messages.single().id)
        assertEquals("conv-2-remote-preview", result.threads[1].messages.single().id)
        assertEquals("hello from remote", result.threads[1].messages.single().body)
    }

    @Test
    fun applyLocalMessageStatus_readMarksIncomingReadAndDecrementsUnread() {
        val initialState = ConversationThreadsState(
            threads = listOf(
                InboxConversationThread(
                    id = "conv-1",
                    title = "Alice",
                    subtitle = "direct",
                    unreadCount = 2,
                    messages = listOf(
                        ChatMessageItem(
                            id = "m-1",
                            senderDisplayName = "Alice",
                            body = "Hi",
                            timestampLabel = "09:30",
                            isFromCurrentUser = false,
                            deliveryStatus = ChatMessageDeliveryStatus.Delivered,
                        )
                    ),
                )
            )
        )

        val result = applyLocalMessageStatus(
            state = initialState,
            conversationId = "conv-1",
            messageId = "m-1",
            targetStatus = ChatMessageDeliveryStatus.Read,
        )

        assertEquals(ChatMessageDeliveryStatus.Read, result.threads.single().messages.single().deliveryStatus)
        assertEquals(1, result.threads.single().unreadCount)
    }

    @Test
    fun applyLocalMessageStatus_deliveredDoesNotDowngradeRead() {
        val initialState = ConversationThreadsState(
            threads = listOf(
                InboxConversationThread(
                    id = "conv-1",
                    title = "Alice",
                    subtitle = "direct",
                    unreadCount = 0,
                    messages = listOf(
                        ChatMessageItem(
                            id = "m-1",
                            senderDisplayName = "Alice",
                            body = "Seen already",
                            timestampLabel = "09:31",
                            isFromCurrentUser = false,
                            deliveryStatus = ChatMessageDeliveryStatus.Read,
                        )
                    ),
                )
            )
        )

        val result = applyLocalMessageStatus(
            state = initialState,
            conversationId = "conv-1",
            messageId = "m-1",
            targetStatus = ChatMessageDeliveryStatus.Delivered,
        )

        assertEquals(ChatMessageDeliveryStatus.Read, result.threads.single().messages.single().deliveryStatus)
    }

    @Test
    fun applyRemoteMessageUpdate_readResponseDecrementsUnreadCount() {
        val initialState = ConversationThreadsState(
            threads = listOf(
                InboxConversationThread(
                    id = "conv-1",
                    title = "Alice",
                    subtitle = "direct",
                    unreadCount = 1,
                    messages = listOf(
                        ChatMessageItem(
                            id = "m-1",
                            senderDisplayName = "Alice",
                            body = "Before remote update",
                            timestampLabel = "09:32",
                            isFromCurrentUser = false,
                            deliveryStatus = ChatMessageDeliveryStatus.Delivered,
                        )
                    ),
                )
            )
        )

        val result = applyRemoteMessageUpdate(
            state = initialState,
            conversationId = "conv-1",
            remoteMessage = ChatMessageItem(
                id = "m-1",
                senderDisplayName = "Alice",
                body = "After remote update",
                timestampLabel = "09:33",
                isFromCurrentUser = false,
                deliveryStatus = ChatMessageDeliveryStatus.Read,
                statusUpdatedAt = "09:33",
            ),
        )

        assertEquals(0, result.threads.single().unreadCount)
        assertEquals("After remote update", result.threads.single().messages.single().body)
        assertEquals(ChatMessageDeliveryStatus.Read, result.threads.single().messages.single().deliveryStatus)
    }

    @Test
    fun clearConversationUnreadCount_onlyTouchesTargetConversation() {
        val initialState = ConversationThreadsState(
            threads = listOf(
                InboxConversationThread(
                    id = "conv-1",
                    title = "Alice",
                    subtitle = "direct",
                    unreadCount = 4,
                    messages = emptyList(),
                ),
                InboxConversationThread(
                    id = "conv-2",
                    title = "Bob",
                    subtitle = "direct",
                    unreadCount = 2,
                    messages = emptyList(),
                ),
            )
        )

        val result = clearConversationUnreadCount(initialState, "conv-1")

        assertEquals(0, result.threads[0].unreadCount)
        assertEquals(2, result.threads[1].unreadCount)
        assertTrue(result.threads[0].messages.isEmpty())
    }
}

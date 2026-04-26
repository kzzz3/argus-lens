package com.kzzz3.argus.lens.data.local

import com.kzzz3.argus.lens.model.conversation.ChatMessageAttachment
import com.kzzz3.argus.lens.model.conversation.ChatMessageDeliveryStatus
import com.kzzz3.argus.lens.model.conversation.ChatMessageItem
import com.kzzz3.argus.lens.model.conversation.InboxConversationThread
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LocalConversationStoreTest {

    @Test
    fun saveAndLoadConversationThreads_preservesMessageAttachmentEnvelope() = runBlocking {
        val dao = FakeLocalConversationDao()
        val store = LocalConversationStore(dao)
        val threads = listOf(
            InboxConversationThread(
                id = "conv-1",
                title = "Files",
                subtitle = "direct",
                unreadCount = 0,
                messages = listOf(
                    ChatMessageItem(
                        id = "m-1",
                        senderDisplayName = "Argus Tester",
                        body = "design-spec.png",
                        timestampLabel = "11:00",
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
        )

        store.saveConversationThreads("tester", threads)
        val restored = store.loadConversationThreads("tester")

        val restoredMessage = restored!!.single().messages.single()
        assertEquals("design-spec.png", restoredMessage.body)
        assertNotNull(restoredMessage.attachment)
        assertEquals("att-1", restoredMessage.attachment?.attachmentId)
        assertEquals("IMAGE", restoredMessage.attachment?.attachmentType)
        assertEquals("design-spec.png", restoredMessage.attachment?.fileName)
    }

    @Test
    fun saveAndLoadConversationThreads_preservesAttachmentWithoutServerId() = runBlocking {
        val dao = FakeLocalConversationDao()
        val store = LocalConversationStore(dao)
        val threads = listOf(
            InboxConversationThread(
                id = "conv-2",
                title = "Pending Uploads",
                subtitle = "direct",
                unreadCount = 0,
                messages = listOf(
                    ChatMessageItem(
                        id = "m-pending",
                        senderDisplayName = "Argus Tester",
                        body = "draft-image.png",
                        timestampLabel = "11:05",
                        isFromCurrentUser = true,
                        deliveryStatus = ChatMessageDeliveryStatus.Sending,
                        attachment = ChatMessageAttachment(
                            attachmentId = null,
                            attachmentType = "IMAGE",
                            fileName = "draft-image.png",
                            contentType = "image/png",
                            contentLength = 0,
                        ),
                    )
                ),
            )
        )

        store.saveConversationThreads("tester", threads)
        val restored = store.loadConversationThreads("tester")

        val restoredMessage = restored!!.single().messages.single()
        assertNotNull(restoredMessage.attachment)
        assertEquals(null, restoredMessage.attachment?.attachmentId)
        assertEquals("IMAGE", restoredMessage.attachment?.attachmentType)
        assertEquals("draft-image.png", restoredMessage.attachment?.fileName)
    }

    private class FakeLocalConversationDao : LocalConversationDao {
        private var conversations: List<LocalConversationEntity> = emptyList()
        private var messages: List<LocalMessageEntity> = emptyList()
        private var draftAttachments: List<LocalDraftAttachmentEntity> = emptyList()

        override suspend fun getConversationsWithMessages(accountId: String): List<LocalConversationWithMessages> {
            return conversations.filter { it.accountId == accountId }.map { conversation ->
                LocalConversationWithMessages(
                    conversation = conversation,
                    messages = messages.filter { it.conversationStorageId == conversation.storageId },
                    draftAttachments = draftAttachments.filter { it.conversationStorageId == conversation.storageId },
                )
            }
        }

        override suspend fun deleteMessagesForAccount(accountId: String) {
            messages = messages.filterNot { it.accountId == accountId }
        }

        override suspend fun deleteConversationsForAccount(accountId: String) {
            conversations = conversations.filterNot { it.accountId == accountId }
        }

        override suspend fun deleteDraftAttachmentsForAccount(accountId: String) {
            draftAttachments = draftAttachments.filterNot { it.accountId == accountId }
        }

        override suspend fun upsertConversations(entities: List<LocalConversationEntity>) {
            conversations = entities
        }

        override suspend fun upsertMessages(entities: List<LocalMessageEntity>) {
            messages = entities
        }

        override suspend fun upsertDraftAttachments(entities: List<LocalDraftAttachmentEntity>) {
            draftAttachments = entities
        }
    }
}

package com.kzzz3.argus.lens.data.conversation

import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.data.session.SessionRepository
import com.kzzz3.argus.lens.model.conversation.ChatMessageAttachment
import com.kzzz3.argus.lens.model.conversation.ChatMessageItem
import com.kzzz3.argus.lens.model.conversation.ChatState
import com.kzzz3.argus.lens.model.conversation.ConversationThreadsState
import com.kzzz3.argus.lens.model.conversation.InboxConversationThread
import com.kzzz3.argus.lens.model.session.AppSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.Response

class RemoteConversationRepositoryTest {

    @Test
    fun refreshConversationMessages_continuesUntilCursorCatchesUp() = runBlocking {
        val initialState = ConversationThreadsState(
            threads = listOf(
                InboxConversationThread(
                    id = "conv-1",
                    title = "Alice",
                    subtitle = "direct",
                    unreadCount = 0,
                    syncCursor = "cursor:conv-1:0:0",
                    messages = emptyList(),
                )
            )
        )
        val localRepository = FakeConversationRepository(initialState)
        val firstPageMessages = (1..50).map { index ->
            RemoteConversationMessage(
                id = "m-$index",
                conversationId = "conv-1",
                senderDisplayName = "Alice",
                body = "Paged message $index",
                timestampLabel = "09:${index.toString().padStart(2, '0')}",
                fromCurrentUser = false,
                deliveryStatus = "DELIVERED",
                statusUpdatedAt = "09:${index.toString().padStart(2, '0')}",
            )
        }
        val apiService = FakeConversationApiService(
            pages = ArrayDeque(
                listOf(
                    RemoteConversationMessagePage(
                        messages = firstPageMessages,
                        nextSyncCursor = "cursor:conv-1:50:51",
                        recentWindowDays = 7,
                        limit = 50,
                    ),
                    RemoteConversationMessagePage(
                        messages = listOf(
                            RemoteConversationMessage(
                                id = "m-51",
                                conversationId = "conv-1",
                                senderDisplayName = "Alice",
                                body = "Paged message 51",
                                timestampLabel = "09:51",
                                fromCurrentUser = false,
                                deliveryStatus = "DELIVERED",
                                statusUpdatedAt = "09:51",
                            ),
                        ),
                        nextSyncCursor = "cursor:conv-1:51:51",
                        recentWindowDays = 7,
                        limit = 50,
                    ),
                )
            )
        )
        val repository = RemoteConversationRepository(
            localRepository = localRepository,
            sessionRepository = FakeSessionRepository(),
            conversationApiService = apiService,
        )

        val refreshedState = repository.refreshConversationMessages(initialState, "conv-1")

        assertEquals(51, refreshedState.threads.single().messages.size)
        assertEquals("m-1", refreshedState.threads.single().messages.first().id)
        assertEquals("m-51", refreshedState.threads.single().messages.last().id)
        assertEquals("cursor:conv-1:51:51", refreshedState.threads.single().syncCursor)
        assertEquals(listOf("cursor:conv-1:0:0", "cursor:conv-1:50:51"), apiService.requestedCursors)
        assertNotNull(localRepository.lastSavedState)
        assertEquals("cursor:conv-1:51:51", localRepository.lastSavedState!!.threads.single().syncCursor)
    }

    @Test
    fun refreshConversationMessages_mapsAttachmentEnvelopeIntoChatMessage() = runBlocking {
        val initialState = ConversationThreadsState(
            threads = listOf(
                InboxConversationThread(
                    id = "conv-attachment",
                    title = "Files",
                    subtitle = "direct",
                    unreadCount = 0,
                    syncCursor = "cursor:conv-attachment:0:0",
                    messages = emptyList(),
                )
            )
        )
        val localRepository = FakeConversationRepository(initialState)
        val apiService = FakeConversationApiService(
            pages = ArrayDeque(
                listOf(
                    RemoteConversationMessagePage(
                        messages = listOf(
                            RemoteConversationMessage(
                                id = "m-file",
                                conversationId = "conv-attachment",
                                senderDisplayName = "Argus Tester",
                                body = "design-spec.png",
                                timestampLabel = "10:15",
                                fromCurrentUser = true,
                                deliveryStatus = "DELIVERED",
                                statusUpdatedAt = "10:15",
                                attachment = RemoteConversationMessageAttachment(
                                    attachmentId = "att-1",
                                    attachmentType = "IMAGE",
                                    fileName = "design-spec.png",
                                    contentType = "image/png",
                                    contentLength = 11,
                                ),
                            ),
                        ),
                        nextSyncCursor = "cursor:conv-attachment:1:1",
                        recentWindowDays = 7,
                        limit = 50,
                    ),
                )
            )
        )
        val repository = RemoteConversationRepository(
            localRepository = localRepository,
            sessionRepository = FakeSessionRepository(),
            conversationApiService = apiService,
        )

        val refreshedState = repository.refreshConversationMessages(initialState, "conv-attachment")

        val message = refreshedState.threads.single().messages.single()
        assertEquals("att-1", message.attachment?.attachmentId)
        assertEquals("IMAGE", message.attachment?.attachmentType)
        assertEquals("design-spec.png", message.attachment?.fileName)
    }

    private class FakeSessionRepository : SessionRepository {
        override suspend fun loadSession(): AppSessionState = AppSessionState(
            isAuthenticated = true,
            accountId = "tester",
            displayName = "Argus Tester",
        )

        override suspend fun loadCredentials(): SessionCredentials = SessionCredentials(accessToken = "token")

        override suspend fun saveSession(state: AppSessionState, credentials: SessionCredentials) = Unit
        override suspend fun clearSession() = Unit
    }

    private class FakeConversationApiService(
        private val pages: ArrayDeque<RemoteConversationMessagePage>,
    ) : ConversationApiService {
        val requestedCursors = mutableListOf<String?>()

        override suspend fun listConversations(
            recentWindowDays: Int,
            authorizationHeader: String,
        ): Response<List<RemoteConversationSummary>> = Response.success(emptyList())

        override suspend fun getConversationDetail(
            conversationId: String,
            authorizationHeader: String,
        ): Response<RemoteConversationDetail> = error("Not used in test")

        override suspend fun listMessages(
            conversationId: String,
            recentWindowDays: Int,
            limit: Int,
            sinceCursor: String?,
            authorizationHeader: String,
        ): Response<RemoteConversationMessagePage> {
            requestedCursors += sinceCursor
            return Response.success(pages.removeFirst())
        }

        override suspend fun sendMessage(
            conversationId: String,
            authorizationHeader: String,
            request: SendRemoteMessageRequest,
        ): Response<RemoteConversationMessage> = error("Not used in test")

        override suspend fun applyReceipt(
            conversationId: String,
            messageId: String,
            authorizationHeader: String,
            request: MessageReceiptRequest,
        ): Response<RemoteConversationMessage> = error("Not used in test")

        override suspend fun recallMessage(
            conversationId: String,
            messageId: String,
            authorizationHeader: String,
            request: Map<String, String>,
        ): Response<RemoteConversationMessage> = error("Not used in test")

        override suspend fun markConversationRead(
            conversationId: String,
            authorizationHeader: String,
            request: Map<String, String>,
        ): Response<RemoteConversationSummary> = error("Not used in test")
    }

    private class FakeConversationRepository(
        private val previewState: ConversationThreadsState,
    ) : ConversationRepository {
        var lastSavedState: ConversationThreadsState? = null

        override fun createPreviewState(currentUserDisplayName: String): ConversationThreadsState = previewState
        override suspend fun loadOrCreateConversationThreads(accountId: String, currentUserDisplayName: String): ConversationThreadsState = previewState
        override suspend fun saveConversationThreads(accountId: String, state: ConversationThreadsState) {
            lastSavedState = state
        }
        override suspend fun clearConversationThreads(accountId: String) = Unit
        override suspend fun refreshConversationMessages(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override suspend fun refreshConversationDetail(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override suspend fun sendMessage(state: ConversationThreadsState, conversationId: String, localMessageId: String, body: String, attachment: ChatMessageAttachment?): ConversationThreadsState = state
        override suspend fun acknowledgeMessageDelivery(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState = state
        override suspend fun acknowledgeMessageRead(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState = state
        override suspend fun recallMessage(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState = state
        override suspend fun markConversationReadRemote(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override fun markConversationAsRead(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override fun updateConversationFromChatState(state: ConversationThreadsState, updatedState: ChatState): ConversationThreadsState = state
        override fun resolveOutgoingMessages(state: ConversationThreadsState, conversationId: String, messageIds: List<String>): ConversationThreadsState = state
        override fun resolveDeliveredMessages(state: ConversationThreadsState, conversationId: String, messageIds: List<String>): ConversationThreadsState = state
    }
}

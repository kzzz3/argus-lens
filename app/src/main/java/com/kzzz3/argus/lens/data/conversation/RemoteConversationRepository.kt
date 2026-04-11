package com.kzzz3.argus.lens.data.conversation

import com.google.gson.Gson
import com.kzzz3.argus.lens.data.session.SessionRepository
import com.kzzz3.argus.lens.feature.inbox.ChatMessageDeliveryStatus
import com.kzzz3.argus.lens.feature.inbox.ChatMessageItem
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import java.io.IOException

class RemoteConversationRepository(
    private val localRepository: ConversationRepository,
    private val sessionRepository: SessionRepository,
    private val conversationApiService: ConversationApiService,
    private val gson: Gson = Gson(),
) : ConversationRepository by localRepository {

    private companion object {
        const val RECENT_WINDOW_DAYS = 7
        const val MESSAGE_PAGE_LIMIT = 50
    }

    override suspend fun loadOrCreateConversationThreads(
        accountId: String,
        currentUserDisplayName: String,
    ): ConversationThreadsState {
        val localState = localRepository.loadOrCreateConversationThreads(accountId, currentUserDisplayName)
        val accessToken = sessionRepository.loadSession().accessToken
        if (accessToken.isBlank()) {
            return localState
        }

        return try {
            val response = conversationApiService.listConversations(
                recentWindowDays = RECENT_WINDOW_DAYS,
                authorizationHeader = "Bearer $accessToken",
            )
            if (!response.isSuccessful) {
                when (parseApiError(response.code(), response.errorBody()?.string().orEmpty())?.code) {
                    "INVALID_CREDENTIALS" -> localState
                    else -> localState
                }
            } else {
                val remoteThreads = response.body().orEmpty().map { summary ->
                    InboxConversationThread(
                        id = summary.id,
                        title = summary.title,
                        subtitle = summary.subtitle,
                        unreadCount = summary.unreadCount,
                        syncCursor = summary.syncCursor,
                        messages = listOf(
                            ChatMessageItem(
                                id = "${summary.id}-remote-preview",
                                senderDisplayName = summary.title,
                                body = summary.preview,
                                timestampLabel = summary.timestampLabel,
                                isFromCurrentUser = false,
                                statusUpdatedAt = summary.timestampLabel,
                            )
                        ),
                    )
                }
                val remoteState = ConversationThreadsState(remoteThreads)
                localRepository.saveConversationThreads(accountId, remoteState)
                remoteState
            }
        } catch (_: IOException) {
            localState
        }
    }

    override suspend fun refreshConversationMessages(
        state: ConversationThreadsState,
        conversationId: String,
    ): ConversationThreadsState {
        val accessToken = sessionRepository.loadSession().accessToken
        if (accessToken.isBlank()) {
            return state
        }

        return try {
            val response = conversationApiService.listMessages(
                conversationId = conversationId,
                recentWindowDays = RECENT_WINDOW_DAYS,
                limit = MESSAGE_PAGE_LIMIT,
                sinceCursor = state.threads.firstOrNull { it.id == conversationId }?.syncCursor?.ifBlank { null },
                authorizationHeader = "Bearer $accessToken",
            )
            if (!response.isSuccessful) {
                when (parseApiError(response.code(), response.errorBody()?.string().orEmpty())?.code) {
                    "CONVERSATION_NOT_FOUND" -> state
                    else -> state
                }
            } else {
                val page = response.body() ?: return state
                val remoteMessages = page.messages.map { message ->
                    ChatMessageItem(
                        id = message.id,
                        senderDisplayName = message.senderDisplayName,
                        body = message.body,
                        timestampLabel = message.timestampLabel,
                        isFromCurrentUser = message.fromCurrentUser,
                        deliveryStatus = parseRemoteDeliveryStatus(message.deliveryStatus),
                        statusUpdatedAt = message.statusUpdatedAt,
                    )
                }

                val nextState = state.copy(
                    threads = state.threads.map { thread ->
                        if (thread.id == conversationId) {
                            thread.copy(
                                messages = mergeMessages(
                                    existingMessages = thread.messages,
                                    incomingMessages = remoteMessages,
                                ),
                                syncCursor = page.nextSyncCursor,
                            )
                        } else {
                            thread
                        }
                    }
                )
                val accountId = sessionRepository.loadSession().accountId
                if (accountId.isNotBlank()) {
                    localRepository.saveConversationThreads(accountId, nextState)
                }
                remoteMessages
                    .filter { !it.isFromCurrentUser && it.deliveryStatus != ChatMessageDeliveryStatus.Delivered }
                    .fold(nextState) { currentState, message ->
                        acknowledgeMessageDelivery(currentState, conversationId, message.id)
                    }
            }
        } catch (_: IOException) {
            state
        }
    }

    override suspend fun acknowledgeMessageRead(
        state: ConversationThreadsState,
        conversationId: String,
        messageId: String,
    ): ConversationThreadsState {
        val session = sessionRepository.loadSession()
        if (session.accessToken.isBlank()) {
            return state
        }

        return try {
            val response = conversationApiService.applyReceipt(
                conversationId = conversationId,
                messageId = messageId,
                authorizationHeader = "Bearer ${session.accessToken}",
                request = MessageReceiptRequest(receiptType = "READ"),
            )
            if (!response.isSuccessful) {
                state
            } else {
                val message = response.body() ?: return state
                val readMessage = ChatMessageItem(
                    id = message.id,
                    senderDisplayName = message.senderDisplayName,
                    body = message.body,
                    timestampLabel = message.timestampLabel,
                    isFromCurrentUser = message.fromCurrentUser,
                    deliveryStatus = parseRemoteDeliveryStatus(message.deliveryStatus),
                    statusUpdatedAt = message.statusUpdatedAt,
                )
                val nextState = state.copy(
                    threads = state.threads.map { thread ->
                        if (thread.id == conversationId) {
                            thread.copy(
                                messages = thread.messages.map { existingMessage ->
                                    if (existingMessage.id == messageId) readMessage else existingMessage
                                }
                            )
                        } else {
                            thread
                        }
                    }
                )
                if (session.accountId.isNotBlank()) {
                    localRepository.saveConversationThreads(session.accountId, nextState)
                }
                nextState
            }
        } catch (_: IOException) {
            state
        }
    }

    override suspend fun sendMessage(
        state: ConversationThreadsState,
        conversationId: String,
        localMessageId: String,
        body: String,
    ): ConversationThreadsState {
        val session = sessionRepository.loadSession()
        if (session.accessToken.isBlank()) {
            return markMessageFailed(state, conversationId, localMessageId)
        }

        return try {
            val response = conversationApiService.sendMessage(
                conversationId = conversationId,
                authorizationHeader = "Bearer ${session.accessToken}",
                request = SendRemoteMessageRequest(body = body),
            )
            if (!response.isSuccessful) {
                when (parseApiError(response.code(), response.errorBody()?.string().orEmpty())?.code) {
                    "CONVERSATION_NOT_FOUND" -> markMessageFailed(state, conversationId, localMessageId)
                    else -> markMessageFailed(state, conversationId, localMessageId)
                }
            } else {
                val message = response.body() ?: return state
                val remoteMessage = ChatMessageItem(
                    id = message.id,
                    senderDisplayName = message.senderDisplayName,
                    body = message.body,
                    timestampLabel = message.timestampLabel,
                    isFromCurrentUser = message.fromCurrentUser,
                    deliveryStatus = parseRemoteDeliveryStatus(message.deliveryStatus),
                    statusUpdatedAt = message.statusUpdatedAt,
                )
                val nextState = state.copy(
                    threads = state.threads.map { thread ->
                        if (thread.id == conversationId) {
                            thread.copy(
                                messages = thread.messages.map { existingMessage ->
                                    if (existingMessage.id == localMessageId) {
                                        remoteMessage
                                    } else {
                                        existingMessage
                                    }
                                }
                            )
                        } else {
                            thread
                        }
                    }
                )
                if (session.accountId.isNotBlank()) {
                    localRepository.saveConversationThreads(session.accountId, nextState)
                }
                nextState
            }
        } catch (_: IOException) {
            markMessageFailed(state, conversationId, localMessageId)
        }
    }

    override suspend fun acknowledgeMessageDelivery(
        state: ConversationThreadsState,
        conversationId: String,
        messageId: String,
    ): ConversationThreadsState {
        val session = sessionRepository.loadSession()
        if (session.accessToken.isBlank()) {
            return state
        }

        return try {
            val response = conversationApiService.applyReceipt(
                conversationId = conversationId,
                messageId = messageId,
                authorizationHeader = "Bearer ${session.accessToken}",
                request = MessageReceiptRequest(receiptType = "DELIVERED"),
            )
            if (!response.isSuccessful) {
                state
            } else {
                val message = response.body() ?: return state
                val deliveredMessage = ChatMessageItem(
                    id = message.id,
                    senderDisplayName = message.senderDisplayName,
                    body = message.body,
                    timestampLabel = message.timestampLabel,
                    isFromCurrentUser = message.fromCurrentUser,
                    deliveryStatus = parseRemoteDeliveryStatus(message.deliveryStatus),
                    statusUpdatedAt = message.statusUpdatedAt,
                )
                val nextState = state.copy(
                    threads = state.threads.map { thread ->
                        if (thread.id == conversationId) {
                            thread.copy(
                                messages = thread.messages.map { existingMessage ->
                                    if (existingMessage.id == messageId) deliveredMessage else existingMessage
                                }
                            )
                        } else {
                            thread
                        }
                    }
                )
                if (session.accountId.isNotBlank()) {
                    localRepository.saveConversationThreads(session.accountId, nextState)
                }
                nextState
            }
        } catch (_: IOException) {
            state
        }
    }

    override suspend fun recallMessage(
        state: ConversationThreadsState,
        conversationId: String,
        messageId: String,
    ): ConversationThreadsState {
        val session = sessionRepository.loadSession()
        if (session.accessToken.isBlank()) {
            return state
        }

        return try {
            val response = conversationApiService.recallMessage(
                conversationId = conversationId,
                messageId = messageId,
                authorizationHeader = "Bearer ${session.accessToken}",
            )
            if (!response.isSuccessful) {
                when (parseApiError(response.code(), response.errorBody()?.string().orEmpty())?.code) {
                    "MESSAGE_NOT_FOUND" -> state
                    "CONVERSATION_NOT_FOUND" -> state
                    else -> state
                }
            } else {
                val message = response.body() ?: return state
                val recalledMessage = ChatMessageItem(
                    id = message.id,
                    senderDisplayName = message.senderDisplayName,
                    body = message.body,
                    timestampLabel = message.timestampLabel,
                    isFromCurrentUser = message.fromCurrentUser,
                    deliveryStatus = parseRemoteDeliveryStatus(message.deliveryStatus),
                    statusUpdatedAt = message.statusUpdatedAt,
                )
                val nextState = state.copy(
                    threads = state.threads.map { thread ->
                        if (thread.id == conversationId) {
                            thread.copy(
                                messages = thread.messages.map { existingMessage ->
                                    if (existingMessage.id == messageId) recalledMessage else existingMessage
                                }
                            )
                        } else {
                            thread
                        }
                    }
                )
                if (session.accountId.isNotBlank()) {
                    localRepository.saveConversationThreads(session.accountId, nextState)
                }
                nextState
            }
        } catch (_: IOException) {
            state
        }
    }

    override suspend fun markConversationReadRemote(
        state: ConversationThreadsState,
        conversationId: String,
    ): ConversationThreadsState {
        val session = sessionRepository.loadSession()
        if (session.accessToken.isBlank()) {
            return state
        }

        return try {
            val response = conversationApiService.markConversationRead(
                conversationId = conversationId,
                authorizationHeader = "Bearer ${session.accessToken}",
            )
            if (!response.isSuccessful) {
                when (parseApiError(response.code(), response.errorBody()?.string().orEmpty())?.code) {
                    "CONVERSATION_NOT_FOUND" -> state
                    else -> state
                }
            } else {
                val summary = response.body() ?: return state
                val nextState = state.copy(
                    threads = state.threads.map { thread ->
                        if (thread.id == conversationId) {
                            thread.copy(
                                title = summary.title,
                                subtitle = summary.subtitle,
                                unreadCount = summary.unreadCount,
                            )
                        } else {
                            thread
                        }
                    }
                )
                if (session.accountId.isNotBlank()) {
                    localRepository.saveConversationThreads(session.accountId, nextState)
                }
                nextState
            }
        } catch (_: IOException) {
            state
        }
    }

    private fun markMessageFailed(
        state: ConversationThreadsState,
        conversationId: String,
        localMessageId: String,
    ): ConversationThreadsState {
        return state.copy(
            threads = state.threads.map { thread ->
                if (thread.id == conversationId) {
                    thread.copy(
                        messages = thread.messages.map { message ->
                            if (message.id == localMessageId) {
                                message.copy(deliveryStatus = ChatMessageDeliveryStatus.Failed)
                            } else {
                                message
                            }
                        }
                    )
                } else {
                    thread
                }
            }
        )
    }

    private fun parseRemoteDeliveryStatus(
        value: String,
    ): ChatMessageDeliveryStatus {
        return when (value.uppercase()) {
            "SENDING" -> ChatMessageDeliveryStatus.Sending
            "SENT" -> ChatMessageDeliveryStatus.Sent
            "DELIVERED" -> ChatMessageDeliveryStatus.Delivered
            "READ" -> ChatMessageDeliveryStatus.Read
            "FAILED" -> ChatMessageDeliveryStatus.Failed
            "RECALLED" -> ChatMessageDeliveryStatus.Recalled
            else -> ChatMessageDeliveryStatus.Delivered
        }
    }

    private fun parseApiError(
        httpCode: Int,
        rawBody: String,
    ): ConversationApiErrorResponse? {
        val parsed = runCatching {
            gson.fromJson(rawBody, ConversationApiErrorResponse::class.java)
        }.getOrNull()

        return if (parsed?.code?.isNotBlank() == true) {
            parsed
        } else {
            when (httpCode) {
                401 -> ConversationApiErrorResponse("INVALID_CREDENTIALS", "Unauthorized")
                404 -> ConversationApiErrorResponse("NOT_FOUND", "Resource not found")
                else -> null
            }
        }
    }

    private fun mergeMessages(
        existingMessages: List<ChatMessageItem>,
        incomingMessages: List<ChatMessageItem>,
    ): List<ChatMessageItem> {
        val messageById = LinkedHashMap<String, ChatMessageItem>()
        existingMessages.forEach { message ->
            messageById[message.id] = message
        }
        incomingMessages.forEach { message ->
            messageById[message.id] = message
        }
        return messageById.values.toList()
    }
}

package com.kzzz3.argus.lens.data.conversation

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
                localState
            } else {
                val remoteThreads = response.body().orEmpty().map { summary ->
                    InboxConversationThread(
                        id = summary.id,
                        title = summary.title,
                        subtitle = summary.subtitle,
                        unreadCount = summary.unreadCount,
                        messages = listOf(
                            ChatMessageItem(
                                id = "${summary.id}-remote-preview",
                                senderDisplayName = summary.title,
                                body = summary.preview,
                                timestampLabel = summary.timestampLabel,
                                isFromCurrentUser = false,
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
                authorizationHeader = "Bearer $accessToken",
            )
            if (!response.isSuccessful) {
                state
            } else {
                val remoteMessages = response.body().orEmpty().map { message ->
                    ChatMessageItem(
                        id = message.id,
                        senderDisplayName = message.senderDisplayName,
                        body = message.body,
                        timestampLabel = message.timestampLabel,
                        isFromCurrentUser = message.fromCurrentUser,
                        deliveryStatus = parseRemoteDeliveryStatus(message.deliveryStatus),
                    )
                }

                val nextState = state.copy(
                    threads = state.threads.map { thread ->
                        if (thread.id == conversationId) {
                            thread.copy(messages = remoteMessages)
                        } else {
                            thread
                        }
                    }
                )
                val accountId = sessionRepository.loadSession().accountId
                if (accountId.isNotBlank()) {
                    localRepository.saveConversationThreads(accountId, nextState)
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
                markMessageFailed(state, conversationId, localMessageId)
            } else {
                val message = response.body() ?: return state
                val remoteMessage = ChatMessageItem(
                    id = message.id,
                    senderDisplayName = message.senderDisplayName,
                    body = message.body,
                    timestampLabel = message.timestampLabel,
                    isFromCurrentUser = message.fromCurrentUser,
                    deliveryStatus = parseRemoteDeliveryStatus(message.deliveryStatus),
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
                state
            } else {
                val message = response.body() ?: return state
                val recalledMessage = ChatMessageItem(
                    id = message.id,
                    senderDisplayName = message.senderDisplayName,
                    body = message.body,
                    timestampLabel = message.timestampLabel,
                    isFromCurrentUser = message.fromCurrentUser,
                    deliveryStatus = parseRemoteDeliveryStatus(message.deliveryStatus),
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
            "FAILED" -> ChatMessageDeliveryStatus.Failed
            "RECALLED" -> ChatMessageDeliveryStatus.Recalled
            else -> ChatMessageDeliveryStatus.Delivered
        }
    }
}

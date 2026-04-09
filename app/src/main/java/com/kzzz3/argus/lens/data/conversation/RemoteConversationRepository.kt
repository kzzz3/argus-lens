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
            val response = conversationApiService.listConversations("Bearer $accessToken")
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

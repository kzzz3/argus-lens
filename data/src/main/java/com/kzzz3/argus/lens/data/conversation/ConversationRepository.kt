package com.kzzz3.argus.lens.data.conversation

import com.kzzz3.argus.lens.model.conversation.ChatMessageAttachment
import com.kzzz3.argus.lens.model.conversation.ChatMessageItem
import com.kzzz3.argus.lens.model.conversation.ChatState
import com.kzzz3.argus.lens.model.conversation.ConversationThreadsState

interface ConversationRepository {
    fun createPreviewState(currentUserDisplayName: String): ConversationThreadsState
    suspend fun loadOrCreateConversationThreads(accountId: String, currentUserDisplayName: String): ConversationThreadsState
    suspend fun saveConversationThreads(accountId: String, state: ConversationThreadsState)
    suspend fun clearConversationThreads(accountId: String)
    suspend fun refreshConversationMessages(state: ConversationThreadsState, conversationId: String): ConversationThreadsState
    suspend fun refreshConversationDetail(state: ConversationThreadsState, conversationId: String): ConversationThreadsState
    suspend fun sendMessage(state: ConversationThreadsState, conversationId: String, localMessageId: String, body: String, attachment: ChatMessageAttachment? = null): ConversationThreadsState
    suspend fun acknowledgeMessageDelivery(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState
    suspend fun acknowledgeMessageRead(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState
    suspend fun recallMessage(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState
    suspend fun markConversationReadRemote(state: ConversationThreadsState, conversationId: String): ConversationThreadsState
    fun markConversationAsRead(state: ConversationThreadsState, conversationId: String): ConversationThreadsState
    fun updateConversationFromChatState(state: ConversationThreadsState, updatedState: ChatState): ConversationThreadsState
    fun resolveOutgoingMessages(state: ConversationThreadsState, conversationId: String, messageIds: List<String>): ConversationThreadsState
    fun resolveDeliveredMessages(state: ConversationThreadsState, conversationId: String, messageIds: List<String>): ConversationThreadsState
}

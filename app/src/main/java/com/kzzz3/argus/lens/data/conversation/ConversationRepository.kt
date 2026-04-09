package com.kzzz3.argus.lens.data.conversation

import com.kzzz3.argus.lens.feature.contacts.ConversationCreationMode
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState

interface ConversationRepository {
    fun createPreviewState(currentUserDisplayName: String): ConversationThreadsState
    suspend fun loadOrCreateConversationThreads(accountId: String, currentUserDisplayName: String): ConversationThreadsState
    suspend fun saveConversationThreads(accountId: String, state: ConversationThreadsState)
    suspend fun clearConversationThreads(accountId: String)
    suspend fun refreshConversationMessages(state: ConversationThreadsState, conversationId: String): ConversationThreadsState
    fun markConversationAsRead(state: ConversationThreadsState, conversationId: String): ConversationThreadsState
    fun updateConversationFromChatState(state: ConversationThreadsState, updatedState: ChatState): ConversationThreadsState
    fun createConversation(state: ConversationThreadsState, displayName: String, mode: ConversationCreationMode): ConversationThreadsState
    fun resolveConversationId(state: ConversationThreadsState, displayName: String): String
    fun resolveOutgoingMessages(state: ConversationThreadsState, conversationId: String, messageIds: List<String>): ConversationThreadsState
    fun resolveDeliveredMessages(state: ConversationThreadsState, conversationId: String, messageIds: List<String>): ConversationThreadsState
}

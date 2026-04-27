package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.OpenInboxConversationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal data class InboxRouteRequest(
    val threadsState: ConversationThreadsState,
)

internal data class InboxRouteCallbacks(
    val onConversationThreadsChanged: (ConversationThreadsState) -> Unit,
    val onConversationOpened: (String) -> Unit,
)

internal class InboxRouteRuntime(
    private val scope: CoroutineScope,
    private val openConversation: (ConversationThreadsState, String) -> OpenInboxConversationResult,
    private val synchronizeConversation: suspend (ConversationThreadsState, String) -> ConversationThreadsState,
) {
    fun openConversation(
        conversationId: String,
        request: InboxRouteRequest,
        callbacks: InboxRouteCallbacks,
    ) {
        val openResult = openConversation(request.threadsState, conversationId)
        callbacks.onConversationThreadsChanged(openResult.conversationThreadsState)
        callbacks.onConversationOpened(openResult.conversationId)
        scope.launch {
            callbacks.onConversationThreadsChanged(
                synchronizeConversation(openResult.conversationThreadsState, conversationId)
            )
        }
    }
}

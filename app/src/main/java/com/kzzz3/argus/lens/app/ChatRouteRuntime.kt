package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.call.CallSessionMode
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.inbox.ChatAction
import com.kzzz3.argus.lens.feature.inbox.ChatActionResult
import com.kzzz3.argus.lens.feature.inbox.ChatCallMode
import com.kzzz3.argus.lens.feature.inbox.ChatDispatchResult
import com.kzzz3.argus.lens.feature.inbox.ChatEffect
import com.kzzz3.argus.lens.feature.inbox.ChatMessageItem
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ChatStatusResult
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal data class ChatRouteRequest(
    val threadsState: ConversationThreadsState,
    val chatState: ChatState,
)

internal data class ChatRouteCallbacks(
    val onRouteChanged: (AppRoute) -> Unit,
    val onConversationThreadsChanged: (ConversationThreadsState) -> Unit,
    val onChatStatusChanged: (String?, Boolean) -> Unit,
    val onCallSessionStateChanged: (CallSessionState) -> Unit,
    val getCurrentRoute: () -> AppRoute,
)

internal class ChatRouteRuntime(
    private val scope: CoroutineScope,
    private val reduceAction: (ConversationThreadsState, ChatState, ChatAction) -> ChatActionResult,
    private val startCall: (String, String, CallSessionMode, (CallSessionState) -> Unit, () -> Unit, () -> Boolean) -> Unit,
    private val dispatchOutgoingMessages: suspend (ConversationThreadsState, String, List<ChatMessageItem>) -> ChatDispatchResult,
    private val downloadAttachment: suspend (String, String) -> ChatStatusResult,
    private val recallMessage: suspend (ConversationThreadsState, ChatState, String) -> ConversationThreadsState,
) {
    fun handleAction(
        action: ChatAction,
        request: ChatRouteRequest,
        callbacks: ChatRouteCallbacks,
    ) {
        val result = reduceAction(request.threadsState, request.chatState, action)
        callbacks.onConversationThreadsChanged(result.conversationThreadsState)
        handleEffect(result, request, callbacks)
        handleActionSideEffect(action, request, callbacks)
    }

    private fun handleEffect(
        result: ChatActionResult,
        request: ChatRouteRequest,
        callbacks: ChatRouteCallbacks,
    ) {
        when (val effect = result.effect) {
            ChatEffect.NavigateBackToInbox -> callbacks.onRouteChanged(AppRoute.Inbox)
            is ChatEffect.StartCall -> {
                startCall(
                    effect.conversationId,
                    effect.contactDisplayName,
                    if (effect.mode == ChatCallMode.Video) CallSessionMode.Video else CallSessionMode.Audio,
                    callbacks.onCallSessionStateChanged,
                    { callbacks.onRouteChanged(AppRoute.CallSession) },
                    { callbacks.getCurrentRoute() == AppRoute.CallSession },
                )
            }
            is ChatEffect.DispatchOutgoingMessages -> {
                scope.launch {
                    val outgoingMessages = result.chatState.messages
                        .filter { message -> message.id in effect.messageIds }
                    val dispatchResult = dispatchOutgoingMessages(
                        request.threadsState,
                        effect.conversationId,
                        outgoingMessages,
                    )
                    callbacks.onConversationThreadsChanged(dispatchResult.conversationThreadsState)
                    val summary = dispatchResult.summary
                    if (summary?.failureMessage != null) {
                        callbacks.onChatStatusChanged(summary.failureMessage, true)
                    }
                }
            }
            null -> Unit
        }
    }

    private fun handleActionSideEffect(
        action: ChatAction,
        request: ChatRouteRequest,
        callbacks: ChatRouteCallbacks,
    ) {
        if (action is ChatAction.DownloadAttachment) {
            scope.launch {
                val result = downloadAttachment(action.attachmentId, action.fileName)
                callbacks.onChatStatusChanged(result.message, result.isError)
            }
        }

        if (action is ChatAction.RecallMessage) {
            scope.launch {
                callbacks.onConversationThreadsChanged(
                    recallMessage(request.threadsState, request.chatState, action.messageId)
                )
            }
        }
    }
}

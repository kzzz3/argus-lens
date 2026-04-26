package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.call.CallSessionMode
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.inbox.ChatAction
import com.kzzz3.argus.lens.feature.inbox.ChatActionResult
import com.kzzz3.argus.lens.feature.inbox.ChatCallMode
import com.kzzz3.argus.lens.feature.inbox.ChatDispatchResult
import com.kzzz3.argus.lens.feature.inbox.ChatEffect
import com.kzzz3.argus.lens.feature.inbox.ChatMessageDeliveryStatus
import com.kzzz3.argus.lens.feature.inbox.ChatMessageItem
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ChatStatusResult
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.OutgoingDispatchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatRouteRuntimeTest {
    @Test
    fun handleAction_navigateBackToInboxRoutesToInboxAfterPublishingThreads() {
        val updatedThreads = ConversationThreadsState()
        val runtime = createRuntime(
            reduceAction = { threads, chat, action ->
                assertEquals(ChatAction.NavigateBackToInbox, action)
                ChatActionResult(updatedThreads, chat, ChatEffect.NavigateBackToInbox)
            },
        )
        var appliedThreads: ConversationThreadsState? = null
        var routedTo: AppRoute? = null

        runtime.handleAction(
            action = ChatAction.NavigateBackToInbox,
            request = chatRequest(),
            callbacks = callbacks(
                onConversationThreadsChanged = { appliedThreads = it },
                onRouteChanged = { routedTo = it },
            ),
        )

        assertEquals(updatedThreads, appliedThreads)
        assertEquals(AppRoute.Inbox, routedTo)
    }

    @Test
    fun handleAction_startAudioAndVideoCallMapsModesAndOpensCallRoute() {
        val starts = mutableListOf<CallStart>()
        val keepTickingResults = mutableListOf<Boolean>()
        var currentRoute = AppRoute.Chat
        val runtime = createRuntime(
            reduceAction = { threads, chat, action ->
                val mode = if (action == ChatAction.StartVideoCall) ChatCallMode.Video else ChatCallMode.Audio
                ChatActionResult(
                    conversationThreadsState = threads,
                    chatState = chat,
                    effect = ChatEffect.StartCall(chat.conversationId, chat.conversationTitle, mode),
                )
            },
            startCall = { conversationId, contactDisplayName, mode, _, openCallSession, shouldKeepTicking ->
                starts += CallStart(conversationId, contactDisplayName, mode)
                keepTickingResults += shouldKeepTicking()
                currentRoute = AppRoute.CallSession
                openCallSession()
                keepTickingResults += shouldKeepTicking()
            },
        )
        val routes = mutableListOf<AppRoute>()

        runtime.handleAction(
            ChatAction.StartAudioCall,
            chatRequest(),
            callbacks(
                onRouteChanged = routes::add,
                getCurrentRoute = { currentRoute },
            ),
        )
        currentRoute = AppRoute.Chat
        runtime.handleAction(
            ChatAction.StartVideoCall,
            chatRequest(),
            callbacks(
                onRouteChanged = routes::add,
                getCurrentRoute = { currentRoute },
            ),
        )

        assertEquals(
            listOf(
                CallStart("conversation-1", "Alice", CallSessionMode.Audio),
                CallStart("conversation-1", "Alice", CallSessionMode.Video),
            ),
            starts,
        )
        assertEquals(listOf(AppRoute.CallSession, AppRoute.CallSession), routes)
        assertEquals(listOf(false, true, false, true), keepTickingResults)
    }

    @Test
    fun handleAction_sendMessageDispatchesOnlyReducerMessageIdsAndPublishesFailureSummary() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val newMessage = message("new-message", ChatMessageDeliveryStatus.Sending)
        val ignoredMessage = message("ignored-message", ChatMessageDeliveryStatus.Sending)
        val reducedChat = chatState(messages = listOf(newMessage, ignoredMessage))
        val requestThreads = ConversationThreadsState()
        val reducerThreads = ConversationThreadsState()
        val dispatchThreads = ConversationThreadsState()
        var dispatchedMessages: List<ChatMessageItem>? = null
        var dispatchedThreads: ConversationThreadsState? = null
        val threadEvents = mutableListOf<ConversationThreadsState>()
        val runtime = createRuntime(
            scope = scope,
            reduceAction = { threads, _, _ ->
                ChatActionResult(
                    conversationThreadsState = reducerThreads,
                    chatState = reducedChat,
                    effect = ChatEffect.DispatchOutgoingMessages("conversation-1", listOf("new-message")),
                )
            },
            dispatchOutgoingMessages = { threads, _, messages ->
                dispatchedThreads = threads
                dispatchedMessages = messages
                ChatDispatchResult(
                    conversationThreadsState = dispatchThreads,
                    summary = OutgoingDispatchResult(dispatchThreads, "Upload failed"),
                )
            },
        )
        var status: Pair<String?, Boolean>? = null
        var appliedThreads: ConversationThreadsState? = null

        runtime.handleAction(
            action = ChatAction.SendMessage,
            request = chatRequest(threadsState = requestThreads),
            callbacks = callbacks(
                onConversationThreadsChanged = {
                    threadEvents += it
                    appliedThreads = it
                },
                onChatStatusChanged = { message, isError -> status = message to isError },
            ),
        )

        assertEquals(requestThreads, dispatchedThreads)
        assertEquals(listOf(newMessage), dispatchedMessages)
        assertEquals(listOf(reducerThreads, dispatchThreads), threadEvents)
        assertEquals(dispatchThreads, appliedThreads)
        assertEquals("Upload failed" to true, status)
        scope.cancel()
    }

    @Test
    fun handleAction_downloadAttachmentPublishesStatus() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val runtime = createRuntime(
            scope = scope,
            downloadAttachment = { attachmentId, fileName ->
                assertEquals("attachment-1", attachmentId)
                assertEquals("receipt.pdf", fileName)
                ChatStatusResult("Downloaded", false)
            },
        )
        var status: Pair<String?, Boolean>? = null

        runtime.handleAction(
            action = ChatAction.DownloadAttachment("attachment-1", "receipt.pdf"),
            request = chatRequest(),
            callbacks = callbacks(onChatStatusChanged = { message, isError -> status = message to isError }),
        )

        assertEquals("Downloaded" to false, status)
        scope.cancel()
    }

    @Test
    fun handleAction_recallMessagePublishesUpdatedThreads() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val requestThreads = ConversationThreadsState()
        val requestChat = chatState()
        val recalledThreads = ConversationThreadsState()
        var recalledInputThreads: ConversationThreadsState? = null
        var recalledInputChat: ChatState? = null
        val runtime = createRuntime(
            scope = scope,
            recallMessage = { threads, chat, messageId ->
                recalledInputThreads = threads
                recalledInputChat = chat
                assertEquals("message-1", messageId)
                recalledThreads
            },
        )
        var appliedThreads: ConversationThreadsState? = null

        runtime.handleAction(
            action = ChatAction.RecallMessage("message-1"),
            request = chatRequest(
                threadsState = requestThreads,
                chatState = requestChat,
            ),
            callbacks = callbacks(onConversationThreadsChanged = { appliedThreads = it }),
        )

        assertEquals(requestThreads, recalledInputThreads)
        assertEquals(requestChat, recalledInputChat)
        assertEquals(recalledThreads, appliedThreads)
        scope.cancel()
    }

    private fun createRuntime(
        scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
        reduceAction: (ConversationThreadsState, ChatState, ChatAction) -> ChatActionResult = { threads, chat, _ -> ChatActionResult(threads, chat, null) },
        startCall: (String, String, CallSessionMode, (CallSessionState) -> Unit, () -> Unit, () -> Boolean) -> Unit = { _, _, _, _, openCallSession, _ -> openCallSession() },
        dispatchOutgoingMessages: suspend (ConversationThreadsState, String, List<ChatMessageItem>) -> ChatDispatchResult = { threads, _, _ -> ChatDispatchResult(threads, null) },
        downloadAttachment: suspend (String, String) -> ChatStatusResult = { _, _ -> ChatStatusResult(null, false) },
        recallMessage: suspend (ConversationThreadsState, ChatState, String) -> ConversationThreadsState = { threads, _, _ -> threads },
    ): ChatRouteRuntime {
        return ChatRouteRuntime(
            scope = scope,
            reduceAction = reduceAction,
            startCall = startCall,
            dispatchOutgoingMessages = dispatchOutgoingMessages,
            downloadAttachment = downloadAttachment,
            recallMessage = recallMessage,
        )
    }

    private fun chatRequest(
        threadsState: ConversationThreadsState = ConversationThreadsState(),
        chatState: ChatState = chatState(),
    ): ChatRouteRequest {
        return ChatRouteRequest(
            threadsState = threadsState,
            chatState = chatState,
        )
    }

    private fun callbacks(
        onRouteChanged: (AppRoute) -> Unit = {},
        onConversationThreadsChanged: (ConversationThreadsState) -> Unit = {},
        onChatStatusChanged: (String?, Boolean) -> Unit = { _, _ -> },
        onCallSessionStateChanged: (CallSessionState) -> Unit = {},
        getCurrentRoute: () -> AppRoute = { AppRoute.Chat },
    ): ChatRouteCallbacks {
        return ChatRouteCallbacks(
            onRouteChanged = onRouteChanged,
            onConversationThreadsChanged = onConversationThreadsChanged,
            onChatStatusChanged = onChatStatusChanged,
            onCallSessionStateChanged = onCallSessionStateChanged,
            getCurrentRoute = getCurrentRoute,
        )
    }

    private fun chatState(
        messages: List<ChatMessageItem> = listOf(message("message-1", ChatMessageDeliveryStatus.Sent)),
    ): ChatState {
        return ChatState(
            conversationId = "conversation-1",
            conversationTitle = "Alice",
            conversationSubtitle = "Online",
            currentUserDisplayName = "Tester",
            messages = messages,
        )
    }

    private fun message(id: String, status: ChatMessageDeliveryStatus): ChatMessageItem {
        return ChatMessageItem(
            id = id,
            senderDisplayName = "Tester",
            body = "Hello",
            timestampLabel = "Now",
            isFromCurrentUser = true,
            deliveryStatus = status,
        )
    }

    private data class CallStart(
        val conversationId: String,
        val contactDisplayName: String,
        val mode: CallSessionMode,
    )
}

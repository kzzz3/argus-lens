package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.OpenInboxConversationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class InboxRouteRuntimeTest {
    @Test
    fun openConversation_publishesReadStateOpensConversationThenPublishesSynchronizedState() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val requestThreads = ConversationThreadsState()
        val openedThreads = ConversationThreadsState()
        val synchronizedThreads = ConversationThreadsState()
        val events = mutableListOf<String>()
        var openInputThreads: ConversationThreadsState? = null
        var openInputConversationId: String? = null
        var synchronizeInputThreads: ConversationThreadsState? = null
        var synchronizeInputConversationId: String? = null
        val runtime = InboxRouteRuntime(
            scope = scope,
            openConversation = { threads, conversationId ->
                openInputThreads = threads
                openInputConversationId = conversationId
                OpenInboxConversationResult(openedThreads, "opened-conversation")
            },
            synchronizeConversation = { threads, conversationId ->
                synchronizeInputThreads = threads
                synchronizeInputConversationId = conversationId
                synchronizedThreads
            },
        )
        val appliedThreads = mutableListOf<ConversationThreadsState>()
        var openedConversationId: String? = null

        runtime.openConversation(
            conversationId = "conversation-1",
            request = InboxRouteRequest(threadsState = requestThreads),
            callbacks = InboxRouteCallbacks(
                onConversationThreadsChanged = {
                    events += "threads"
                    appliedThreads += it
                },
                onConversationOpened = {
                    events += "opened"
                    openedConversationId = it
                },
            ),
        )

        assertEquals(requestThreads, openInputThreads)
        assertEquals("conversation-1", openInputConversationId)
        assertEquals(openedThreads, synchronizeInputThreads)
        assertEquals("conversation-1", synchronizeInputConversationId)
        assertEquals("opened-conversation", openedConversationId)
        assertEquals(listOf(openedThreads, synchronizedThreads), appliedThreads)
        assertEquals(listOf("threads", "opened", "threads"), events)
        scope.cancel()
    }
}

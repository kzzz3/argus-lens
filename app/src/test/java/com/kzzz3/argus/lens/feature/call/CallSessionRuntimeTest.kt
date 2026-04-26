package com.kzzz3.argus.lens.feature.call

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CallSessionRuntimeTest {
    @Test
    fun startCall_createsConnectingStateAndRoutesToCallSession() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val runtime = CallSessionRuntime(
            scope = scope,
            activationDelayMillis = Long.MAX_VALUE,
            tickDelayMillis = Long.MAX_VALUE,
        )
        var state = CallSessionState()
        var route: String? = null

        runtime.startCall(
            conversationId = "conv-1",
            contactDisplayName = "Alice",
            mode = CallSessionMode.Audio,
            setState = { state = it },
            openCallSession = { route = "call" },
            shouldKeepTicking = { true },
        )

        assertEquals(CallSessionStatus.Connecting, state.status)
        assertEquals("Alice", state.contactDisplayName)
        assertEquals("call", route)
        runtime.cancel()
        scope.cancel()
    }

    @Test
    fun endCall_cancelsActiveJobAndReturnsToChat() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val runtime = CallSessionRuntime(
            scope = scope,
            activationDelayMillis = Long.MAX_VALUE,
            tickDelayMillis = Long.MAX_VALUE,
            returnToChatDelayMillis = 0L,
        )
        var returnedToChat = false

        runtime.startCall(
            conversationId = "conv-1",
            contactDisplayName = "Alice",
            mode = CallSessionMode.Video,
            setState = {},
            openCallSession = {},
            shouldKeepTicking = { true },
        )
        runtime.endCall(
            currentState = startCallSession("conv-1", "Alice", CallSessionMode.Video),
            setState = {},
            openChat = { returnedToChat = true },
        )

        assertEquals(true, returnedToChat)
        scope.cancel()
    }
}

package com.kzzz3.argus.lens.feature.call

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CallSessionTimerTest {
    @Test
    fun startCall_createsConnectingStateAndRoutesToCallSession() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val timer = CallSessionTimer(
            scope = scope,
            activationDelayMillis = Long.MAX_VALUE,
            tickDelayMillis = Long.MAX_VALUE,
        )
        var state = CallSessionState()
        var route: String? = null

        timer.startCall(
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
        timer.cancel()
        scope.cancel()
    }

    @Test
    fun endCall_cancelsActiveJobAndReturnsToChat() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val timer = CallSessionTimer(
            scope = scope,
            activationDelayMillis = Long.MAX_VALUE,
            tickDelayMillis = Long.MAX_VALUE,
            returnToChatDelayMillis = 0L,
        )
        var returnedToChat = false

        timer.startCall(
            conversationId = "conv-1",
            contactDisplayName = "Alice",
            mode = CallSessionMode.Video,
            setState = {},
            openCallSession = {},
            shouldKeepTicking = { true },
        )
        timer.endCall(
            currentState = startCallSession("conv-1", "Alice", CallSessionMode.Video),
            setState = {},
            openChat = { returnedToChat = true },
        )

        assertEquals(true, returnedToChat)
        scope.cancel()
    }

    @Test
    fun cancel_afterEndCallPreventsDelayedReturnToChat() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val timer = CallSessionTimer(
            scope = scope,
            activationDelayMillis = Long.MAX_VALUE,
            tickDelayMillis = Long.MAX_VALUE,
            returnToChatDelayMillis = Long.MAX_VALUE,
        )
        var returnedToChat = false

        timer.startCall(
            conversationId = "conv-1",
            contactDisplayName = "Alice",
            mode = CallSessionMode.Video,
            setState = {},
            openCallSession = {},
            shouldKeepTicking = { true },
        )
        timer.endCall(
            currentState = startCallSession("conv-1", "Alice", CallSessionMode.Video),
            setState = {},
            openChat = { returnedToChat = true },
        )
        timer.cancel()

        assertEquals(false, returnedToChat)
        scope.cancel()
    }
}

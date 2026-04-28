package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.call.CallSessionAction
import com.kzzz3.argus.lens.feature.call.CallSessionRouteCallbacks
import com.kzzz3.argus.lens.feature.call.CallSessionRouteHandler
import com.kzzz3.argus.lens.feature.call.CallSessionRouteRequest
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.call.CallSessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallSessionRouteHandlerTest {
    @Test
    fun handleAction_nonEndActionPublishesReducedStateWithoutEndingCall() {
        val currentState = CallSessionState(isMuted = false)
        val reducedState = currentState.copy(isMuted = true)
        var reducedInputState: CallSessionState? = null
        var reducedInputAction: CallSessionAction? = null
        var endedState: CallSessionState? = null
        var publishedState: CallSessionState? = null
        var routedTo: AppRoute? = null
        val handler = CallSessionRouteHandler(
            reduceAction = { state, action ->
                reducedInputState = state
                reducedInputAction = action
                reducedState
            },
            endCall = { state, _, _ -> endedState = state },
        )

        handler.handleAction(
            action = CallSessionAction.ToggleMute,
            request = CallSessionRouteRequest(currentState),
            callbacks = CallSessionRouteCallbacks(
                onCallSessionStateChanged = { publishedState = it },
                onNavigateBackToChat = { routedTo = AppRoute.Chat },
            ),
        )

        assertEquals(currentState, reducedInputState)
        assertEquals(CallSessionAction.ToggleMute, reducedInputAction)
        assertEquals(reducedState, publishedState)
        assertNull(endedState)
        assertNull(routedTo)
    }

    @Test
    fun handleAction_endCallPublishesReducedStateThenDelegatesEndCallBackToChat() {
        val currentState = CallSessionState(status = CallSessionStatus.Active)
        val reducedState = currentState.copy(status = CallSessionStatus.Ended)
        val handlerEndedState = reducedState.copy(durationLabel = "00:42")
        val events = mutableListOf<String>()
        var endedInputState: CallSessionState? = null
        var publishedState: CallSessionState? = null
        var routedTo: AppRoute? = null
        val handler = CallSessionRouteHandler(
            reduceAction = { _, _ -> reducedState },
            endCall = { state, setState, openChat ->
                events += "end"
                endedInputState = state
                setState(handlerEndedState)
                openChat()
            },
        )

        handler.handleAction(
            action = CallSessionAction.EndCall,
            request = CallSessionRouteRequest(currentState),
            callbacks = CallSessionRouteCallbacks(
                onCallSessionStateChanged = {
                    events += "state"
                    publishedState = it
                },
                onNavigateBackToChat = {
                    events += "route"
                    routedTo = AppRoute.Chat
                },
            ),
        )

        assertEquals(reducedState, endedInputState)
        assertEquals(handlerEndedState, publishedState)
        assertEquals(AppRoute.Chat, routedTo)
        assertEquals(listOf("state", "end", "state", "route"), events)
    }
}

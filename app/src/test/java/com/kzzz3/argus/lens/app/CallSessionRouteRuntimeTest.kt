package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.call.CallSessionAction
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.call.CallSessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallSessionRouteRuntimeTest {
    @Test
    fun handleAction_nonEndActionPublishesReducedStateWithoutEndingCall() {
        val currentState = CallSessionState(isMuted = false)
        val reducedState = currentState.copy(isMuted = true)
        var reducedInputState: CallSessionState? = null
        var reducedInputAction: CallSessionAction? = null
        var endedState: CallSessionState? = null
        var publishedState: CallSessionState? = null
        var routedTo: AppRoute? = null
        val runtime = CallSessionRouteRuntime(
            reduceAction = { state, action ->
                reducedInputState = state
                reducedInputAction = action
                reducedState
            },
            endCall = { state, _, _ -> endedState = state },
        )

        runtime.handleAction(
            action = CallSessionAction.ToggleMute,
            request = CallSessionRouteRequest(currentState),
            callbacks = CallSessionRouteCallbacks(
                onCallSessionStateChanged = { publishedState = it },
                onRouteChanged = { routedTo = it },
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
        val runtimeEndedState = reducedState.copy(durationLabel = "00:42")
        val events = mutableListOf<String>()
        var endedInputState: CallSessionState? = null
        var publishedState: CallSessionState? = null
        var routedTo: AppRoute? = null
        val runtime = CallSessionRouteRuntime(
            reduceAction = { _, _ -> reducedState },
            endCall = { state, setState, openChat ->
                events += "end"
                endedInputState = state
                setState(runtimeEndedState)
                openChat()
            },
        )

        runtime.handleAction(
            action = CallSessionAction.EndCall,
            request = CallSessionRouteRequest(currentState),
            callbacks = CallSessionRouteCallbacks(
                onCallSessionStateChanged = {
                    events += "state"
                    publishedState = it
                },
                onRouteChanged = {
                    events += "route"
                    routedTo = it
                },
            ),
        )

        assertEquals(reducedState, endedInputState)
        assertEquals(runtimeEndedState, publishedState)
        assertEquals(AppRoute.Chat, routedTo)
        assertEquals(listOf("state", "end", "state", "route"), events)
    }
}

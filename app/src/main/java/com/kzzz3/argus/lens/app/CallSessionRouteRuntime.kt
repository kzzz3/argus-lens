package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.call.CallSessionAction
import com.kzzz3.argus.lens.feature.call.CallSessionState

internal data class CallSessionRouteRequest(
    val currentState: CallSessionState,
)

internal data class CallSessionRouteCallbacks(
    val onCallSessionStateChanged: (CallSessionState) -> Unit,
    val onRouteChanged: (AppRoute) -> Unit,
)

internal class CallSessionRouteRuntime(
    private val reduceAction: (CallSessionState, CallSessionAction) -> CallSessionState,
    private val endCall: (CallSessionState, (CallSessionState) -> Unit, () -> Unit) -> Unit,
) {
    fun handleAction(
        action: CallSessionAction,
        request: CallSessionRouteRequest,
        callbacks: CallSessionRouteCallbacks,
    ) {
        val nextState = reduceAction(request.currentState, action)
        callbacks.onCallSessionStateChanged(nextState)
        if (action == CallSessionAction.EndCall) {
            endCall(
                nextState,
                callbacks.onCallSessionStateChanged,
                { callbacks.onRouteChanged(AppRoute.Chat) },
            )
        }
    }
}

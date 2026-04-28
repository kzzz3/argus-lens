package com.kzzz3.argus.lens.feature.call

data class CallSessionRouteRequest(
    val currentState: CallSessionState,
)

data class CallSessionRouteCallbacks(
    val onCallSessionStateChanged: (CallSessionState) -> Unit,
    val onNavigateBackToChat: () -> Unit,
)

class CallSessionRouteHandler(
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
                callbacks.onNavigateBackToChat,
            )
        }
    }
}

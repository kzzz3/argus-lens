package com.kzzz3.argus.lens.feature.call

data class CallSessionFeatureSnapshot(
    val currentState: CallSessionState,
)

data class CallSessionFeatureCallbacks(
    val onCallSessionStateChanged: (CallSessionState) -> Unit,
    val onNavigateBackToChat: () -> Unit,
)

class CallSessionFeatureController(
    private val routeHandler: CallSessionRouteHandler,
) {
    fun handleAction(
        action: CallSessionAction,
        snapshot: CallSessionFeatureSnapshot,
        callbacks: CallSessionFeatureCallbacks,
    ) {
        routeHandler.handleAction(
            action = action,
            request = CallSessionRouteRequest(currentState = snapshot.currentState),
            callbacks = CallSessionRouteCallbacks(
                onCallSessionStateChanged = callbacks.onCallSessionStateChanged,
                onNavigateBackToChat = callbacks.onNavigateBackToChat,
            ),
        )
    }
}

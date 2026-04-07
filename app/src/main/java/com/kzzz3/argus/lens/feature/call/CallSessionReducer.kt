package com.kzzz3.argus.lens.feature.call

fun reduceCallSessionState(
    currentState: CallSessionState,
    action: CallSessionAction,
): CallSessionState {
    return when (action) {
        CallSessionAction.ToggleMute -> currentState.copy(isMuted = !currentState.isMuted)
        CallSessionAction.ToggleSpeaker -> currentState.copy(isSpeakerEnabled = !currentState.isSpeakerEnabled)
        CallSessionAction.ToggleCamera -> currentState.copy(isCameraEnabled = !currentState.isCameraEnabled)
        CallSessionAction.EndCall -> currentState.copy(status = CallSessionStatus.Ended)
    }
}

fun createInitialCallSessionState(
    conversationId: String,
    contactDisplayName: String,
    mode: CallSessionMode,
): CallSessionState {
    return CallSessionState(
        conversationId = conversationId,
        contactDisplayName = contactDisplayName,
        mode = mode,
        status = CallSessionStatus.Connecting,
        durationLabel = "00:00",
        isSpeakerEnabled = mode == CallSessionMode.Video,
        isCameraEnabled = mode == CallSessionMode.Video,
    )
}

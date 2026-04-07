package com.kzzz3.argus.lens.feature.call

fun createCallSessionUiState(
    state: CallSessionState,
): CallSessionUiState {
    return CallSessionUiState(
        title = state.contactDisplayName,
        subtitle = "Stage-1 local call shell",
        modeLabel = if (state.mode == CallSessionMode.Video) "Video call" else "Audio call",
        statusLabel = when (state.status) {
            CallSessionStatus.Connecting -> "Connecting locally..."
            CallSessionStatus.Active -> "Call active"
            CallSessionStatus.Ended -> "Call ended"
        },
        durationLabel = state.durationLabel,
        muteActionLabel = if (state.isMuted) "Unmute" else "Mute",
        speakerActionLabel = if (state.isSpeakerEnabled) "Speaker off" else "Speaker on",
        cameraActionLabel = if (state.isCameraEnabled) "Camera off" else "Camera on",
        endCallActionLabel = if (state.status == CallSessionStatus.Ended) "Back to chat" else "End call",
        isCameraActionVisible = state.mode == CallSessionMode.Video,
    )
}

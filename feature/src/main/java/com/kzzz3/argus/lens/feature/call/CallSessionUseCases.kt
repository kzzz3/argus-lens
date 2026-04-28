package com.kzzz3.argus.lens.feature.call

fun startCallSession(
    conversationId: String,
    contactDisplayName: String,
    mode: CallSessionMode,
): CallSessionState {
    return createInitialCallSessionState(
        conversationId = conversationId,
        contactDisplayName = contactDisplayName,
        mode = mode,
    )
}

fun activateConnectingCallSession(state: CallSessionState): CallSessionState {
    return if (state.status == CallSessionStatus.Connecting) {
        state.copy(status = CallSessionStatus.Active)
    } else {
        state
    }
}

fun tickActiveCallSession(state: CallSessionState): CallSessionState {
    return if (state.status == CallSessionStatus.Active) {
        state.copy(durationLabel = incrementDurationLabel(state.durationLabel))
    } else {
        state
    }
}

fun incrementDurationLabel(durationLabel: String): String {
    val parts = durationLabel.split(":")
    val minutes = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val seconds = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val totalSeconds = minutes * 60 + seconds + 1
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

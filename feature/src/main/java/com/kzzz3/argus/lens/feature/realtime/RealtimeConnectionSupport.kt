package com.kzzz3.argus.lens.feature.realtime

import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeConnectionState

fun buildRealtimeStatusLabel(state: ConversationRealtimeConnectionState): String {
    return when (state) {
        ConversationRealtimeConnectionState.DISABLED -> "offline"
        ConversationRealtimeConnectionState.CONNECTING -> "connecting"
        ConversationRealtimeConnectionState.LIVE -> "live"
        ConversationRealtimeConnectionState.RECOVERING -> "recovering"
    }
}

fun realtimeReconnectDelayMillis(attempt: Int): Long {
    return when {
        attempt <= 1 -> 1_000L
        attempt == 2 -> 2_000L
        attempt == 3 -> 4_000L
        else -> 8_000L
    }
}

fun isSseAuthFailure(throwable: Throwable): Boolean {
    var current: Throwable? = throwable
    while (current != null) {
        val message = current.message.orEmpty()
        if (message.contains("HTTP 401", ignoreCase = true) || message.contains("HTTP 403", ignoreCase = true)) {
            return true
        }
        current = current.cause
    }
    return false
}

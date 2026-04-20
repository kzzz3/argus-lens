package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.session.AppSessionState

internal fun shouldApplyWalletRequestResult(
    currentSession: AppSessionState,
    requestAccountId: String,
    requestGeneration: Int,
    activeGeneration: Int,
): Boolean {
    return requestGeneration == activeGeneration &&
        currentSession.isAuthenticated &&
        requestAccountId.isNotBlank() &&
        currentSession.accountId == requestAccountId
}

internal fun incrementCallDurationLabel(
    currentLabel: String,
): String {
    val parts = currentLabel.split(":")
    if (parts.size != 2) return "00:01"

    val minutes = parts[0].toIntOrNull() ?: 0
    val seconds = parts[1].toIntOrNull() ?: 0
    val totalSeconds = minutes * 60 + seconds + 1
    val nextMinutes = totalSeconds / 60
    val nextSeconds = totalSeconds % 60
    return "%02d:%02d".format(nextMinutes, nextSeconds)
}

internal fun isSseAuthFailure(throwable: Throwable): Boolean {
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

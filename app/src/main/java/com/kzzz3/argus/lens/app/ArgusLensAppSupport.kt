package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.app.session.createAuthenticatedSession
import com.kzzz3.argus.lens.data.auth.AuthSession
import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.register.RegisterFormState

internal const val DEFAULT_PREVIEW_DISPLAY_NAME = "Argus Tester"

internal data class PostAuthUiState(
    val callSessionState: com.kzzz3.argus.lens.feature.call.CallSessionState,
    val conversationThreadsState: com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState,
    val hydratedConversationAccountId: String,
    val selectedConversationId: String,
    val nextAuthFormState: AuthFormState,
    val realtimeReconnectIncrement: Int = 1,
)

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

internal fun createSessionFromAuthSession(
    session: AuthSession,
    fallbackRefreshToken: String = "",
): AppSessionState {
    return createAuthenticatedSession(
        accountId = session.accountId,
        displayName = session.displayName,
        accessToken = session.accessToken,
        refreshToken = session.refreshToken.ifBlank { fallbackRefreshToken },
    )
}

internal fun resolvePreviewDisplayName(displayName: String): String {
    return displayName.ifBlank { DEFAULT_PREVIEW_DISPLAY_NAME }
}

internal fun completeRegistrationForm(
    formState: RegisterFormState,
    submitResult: String,
): RegisterFormState {
    return formState.copy(
        isSubmitting = false,
        submitResult = submitResult,
    )
}

internal fun completeAuthForm(
    formState: AuthFormState,
    submitResult: String,
): AuthFormState {
    return formState.copy(
        isSubmitting = false,
        submitResult = submitResult,
    )
}

internal fun createPostAuthUiState(
    signedInState: AppSignedInState,
    accountId: String,
): PostAuthUiState {
    return PostAuthUiState(
        callSessionState = signedInState.callSessionState,
        conversationThreadsState = signedInState.conversationThreadsState,
        hydratedConversationAccountId = signedInState.hydratedConversationAccountId,
        selectedConversationId = signedInState.selectedConversationId,
        nextAuthFormState = AuthFormState(account = accountId),
    )
}

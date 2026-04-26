package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.app.session.createAuthenticatedSession
import com.kzzz3.argus.lens.data.auth.AuthSession
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.feature.wallet.WalletState

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

internal fun createSessionFromAuthSession(
    session: AuthSession,
): AppSessionState {
    return createAuthenticatedSession(
        accountId = session.accountId,
        displayName = session.displayName,
    )
}

internal fun createSessionCredentialsFromAuthSession(
    session: AuthSession,
    fallbackRefreshToken: String = "",
): SessionCredentials {
    return SessionCredentials(
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

internal inline fun applyWalletRequestResult(
    currentState: WalletState,
    isActive: Boolean,
    transform: (WalletState) -> WalletState,
): WalletState {
    return if (isActive) transform(currentState) else currentState
}

package com.kzzz3.argus.lens.app.state

import com.kzzz3.argus.lens.app.AppSignedInState
import com.kzzz3.argus.lens.core.data.auth.AuthSession
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.model.session.AppSessionState
import com.kzzz3.argus.lens.model.session.createAuthenticatedSession

internal const val DEFAULT_PREVIEW_DISPLAY_NAME = "Argus Tester"

internal data class PostAuthUiState(
    val callSessionState: com.kzzz3.argus.lens.feature.call.CallSessionState,
    val conversationThreadsState: com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState,
    val hydratedConversationAccountId: String,
    val nextAuthFormState: AuthFormState,
    val realtimeReconnectIncrement: Int = 1,
)

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
        nextAuthFormState = AuthFormState(account = accountId),
    )
}

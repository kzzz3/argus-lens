package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.model.session.AppSessionState

internal data class AppSessionBoundaryCallbacks(
    val onHydratedConversationAccountChanged: (String?) -> Unit,
    val onCallSessionStateChanged: (CallSessionState) -> Unit,
    val onWalletStateChanged: (WalletState) -> Unit,
    val onConversationThreadsChanged: (ConversationThreadsState) -> Unit,
    val onAuthenticatedSessionApplied: (AppSessionState, SessionCredentials, String, Int) -> Unit,
    val onAuthFormStateChanged: (AuthFormState) -> Unit,
    val onSessionCleared: () -> Unit,
    val onRegisterFormStateChanged: (RegisterFormState) -> Unit,
    val onContactsStateChanged: (ContactsState) -> Unit,
    val onFriendsChanged: (List<FriendEntry>) -> Unit,
    val onFriendRequestStatusReset: () -> Unit,
)

internal class AppSessionBoundaryRuntime(
    private val appShellCoordinator: AppShellCoordinator,
    private val refreshSessionOnce: suspend (AppSessionState, (AppSessionState) -> Unit) -> AuthRepositoryResult,
    private val startSessionRefreshLoop: (() -> Unit) -> Unit,
    private val cancelSessionRefreshLoop: () -> Unit,
    private val invalidateWalletRequests: () -> Unit,
    private val cancelCallSession: () -> Unit,
) {
    suspend fun applySuccessfulAuthResult(
        authResult: AuthRepositoryResult.Success,
        keepSubmitMessageOnAuthForm: Boolean,
        callbacks: AppSessionBoundaryCallbacks,
    ) {
        val authenticatedSession = createSessionFromAuthSession(authResult.session)
        val authenticatedCredentials = createSessionCredentialsFromAuthSession(authResult.session)
        invalidateWalletRequests()
        callbacks.onHydratedConversationAccountChanged(null)
        cancelCallSession()
        val signedInState = appShellCoordinator.handleSignedIn(authenticatedSession)
        val postAuthUiState = createPostAuthUiState(
            signedInState = signedInState,
            accountId = authResult.session.accountId,
        )
        callbacks.onCallSessionStateChanged(postAuthUiState.callSessionState)
        callbacks.onWalletStateChanged(WalletState())
        callbacks.onConversationThreadsChanged(postAuthUiState.conversationThreadsState)
        callbacks.onAuthenticatedSessionApplied(
            authenticatedSession,
            authenticatedCredentials,
            postAuthUiState.hydratedConversationAccountId,
            postAuthUiState.realtimeReconnectIncrement,
        )
        callbacks.onAuthFormStateChanged(if (keepSubmitMessageOnAuthForm) {
            postAuthUiState.nextAuthFormState.copy(submitResult = authResult.session.message)
        } else {
            postAuthUiState.nextAuthFormState
        })
    }

    fun signOutToEntry(
        currentSession: AppSessionState,
        previewThreadsState: ConversationThreadsState,
        message: String?,
        callbacks: AppSessionBoundaryCallbacks,
    ) {
        val signedOutState = appShellCoordinator.createSignedOutState(
            previewThreadsState = previewThreadsState,
            signedOutAccountId = currentSession.accountId,
        )
        cancelSessionRefreshLoop()
        invalidateWalletRequests()
        callbacks.onSessionCleared()
        callbacks.onAuthFormStateChanged(signedOutState.authFormState.copy(submitResult = message))
        callbacks.onRegisterFormStateChanged(signedOutState.registerFormState)
        callbacks.onContactsStateChanged(signedOutState.contactsState)
        cancelCallSession()
        callbacks.onCallSessionStateChanged(signedOutState.callSessionState)
        callbacks.onWalletStateChanged(WalletState())
        callbacks.onConversationThreadsChanged(signedOutState.conversationThreadsState)
        callbacks.onFriendsChanged(emptyList())
        callbacks.onFriendRequestStatusReset()
    }

    suspend fun refreshSessionTokens(
        session: AppSessionState,
        setSession: (AppSessionState) -> Unit,
    ): AuthRepositoryResult {
        return refreshSessionOnce(session, setSession)
    }

    fun scheduleSessionRefreshLoop(onUnauthorized: () -> Unit) {
        startSessionRefreshLoop(onUnauthorized)
    }

    suspend fun applySessionBoundary(
        session: AppSessionState,
        callbacks: AppSessionBoundaryCallbacks,
    ) {
        if (session.isAuthenticated) {
            callbacks.onHydratedConversationAccountChanged(null)
            val signedInState = appShellCoordinator.handleSignedIn(session)
            callbacks.onConversationThreadsChanged(signedInState.conversationThreadsState)
            callbacks.onHydratedConversationAccountChanged(signedInState.hydratedConversationAccountId)
        } else {
            callbacks.onHydratedConversationAccountChanged(null)
        }
    }
}

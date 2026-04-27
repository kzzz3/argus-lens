package com.kzzz3.argus.lens.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.contacts.FriendRequestStatusState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.model.session.AppSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@HiltViewModel
class ArgusLensAppViewModel @Inject constructor(
    val dependencies: AppDependencies,
) : ViewModel() {
    val runtimeScope: CoroutineScope = viewModelScope

    private val _uiState = MutableStateFlow(
        ArgusLensAppUiState(
            appSessionState = dependencies.initialSessionSnapshot,
            conversationThreadsState = dependencies.appShellCoordinator.createPreviewConversationThreads(
                currentUserDisplayName = DEFAULT_PREVIEW_DISPLAY_NAME,
            ),
            currentRoute = resolveInitialAppRoute(
                session = dependencies.initialSessionSnapshot,
                credentials = dependencies.initialSessionCredentials,
            ),
            hydratedConversationAccountId = resolveInitialHydratedConversationAccountId(
                session = dependencies.initialSessionSnapshot,
            ),
        )
    )
    val uiState: StateFlow<ArgusLensAppUiState> = _uiState.asStateFlow()

    fun openRoute(route: AppRoute) {
        _uiState.update { state -> state.copy(currentRoute = route) }
    }

    fun openConversation(conversationId: String) {
        _uiState.update { state ->
            state.copy(
                currentRoute = AppRoute.Chat,
                selectedConversationId = conversationId,
            )
        }
    }

    fun updateChatStatus(message: String?, isError: Boolean) {
        _uiState.update { state ->
            state.copy(
                chatStatusMessage = message,
                chatStatusError = isError,
            )
        }
    }

    fun clearChatStatus() {
        updateChatStatus(message = null, isError = false)
    }

    fun updateFriendRequestStatus(statusState: FriendRequestStatusState) {
        _uiState.update { state ->
            state.copy(
                friendRequestsSnapshot = statusState.snapshot,
                friendRequestsStatusMessage = statusState.message,
                friendRequestsStatusError = statusState.isError,
            )
        }
    }

    fun updateFriendRequestsSnapshot(snapshot: FriendRequestsSnapshot) {
        _uiState.update { state -> state.copy(friendRequestsSnapshot = snapshot) }
    }

    fun resetFriendRequestStatus() {
        _uiState.update { state ->
            state.copy(
                friendRequestsSnapshot = FriendRequestsSnapshot(emptyList(), emptyList()),
                friendRequestsStatusMessage = null,
                friendRequestsStatusError = false,
            )
        }
    }

    fun updateCallSessionState(callSessionState: CallSessionState) {
        _uiState.update { state -> state.copy(callSessionState = callSessionState) }
    }

    fun updateContactsState(contactsState: ContactsState) {
        _uiState.update { state -> state.copy(contactsState = contactsState) }
    }

    fun updateWalletState(walletState: WalletState) {
        _uiState.update { state -> state.copy(walletState = walletState) }
    }

    fun updateFriends(friends: List<FriendEntry>) {
        _uiState.update { state -> state.copy(friends = friends) }
    }

    fun updateConversationThreadsState(conversationThreadsState: ConversationThreadsState) {
        _uiState.update { state -> state.copy(conversationThreadsState = conversationThreadsState) }
    }

    fun updateAuthFormState(formState: AuthFormState) {
        _uiState.update { state -> state.copy(authFormState = formState) }
    }

    fun updateRegisterFormState(formState: RegisterFormState) {
        _uiState.update { state -> state.copy(registerFormState = formState) }
    }

    fun applyHydratedSession(
        session: AppSessionState,
        hydratedConversationAccountId: String?,
    ) {
        _uiState.update { state ->
            applyHydratedSessionTransition(state, session, hydratedConversationAccountId)
        }
    }

    fun applyAuthenticatedSession(
        session: AppSessionState,
        credentials: SessionCredentials,
        hydratedConversationAccountId: String,
        realtimeReconnectIncrement: Int,
    ) {
        dependencies.sessionCredentialsStore.update(credentials)
        _uiState.update { state ->
            applyAuthenticatedSessionTransition(
                state = state,
                session = session,
                hydratedConversationAccountId = hydratedConversationAccountId,
                realtimeReconnectIncrement = realtimeReconnectIncrement,
            )
        }
    }

    fun applyRefreshedSession(session: AppSessionState) {
        _uiState.update { state -> applyRefreshedSessionTransition(state, session) }
    }

    fun clearSession() {
        dependencies.sessionCredentialsStore.clear()
        _uiState.update(::applySessionClearedTransition)
    }

    fun updateHydratedConversationAccountId(accountId: String?) {
        _uiState.update { state -> state.copy(hydratedConversationAccountId = accountId) }
    }

    fun updateRealtimeConnectionState(connectionState: ConversationRealtimeConnectionState) {
        _uiState.update { state -> state.copy(realtimeConnectionState = connectionState) }
    }

    fun recordRealtimeEventId(eventId: String) {
        if (eventId.isBlank()) return
        _uiState.update { state -> state.copy(realtimeLastEventId = eventId) }
    }

    fun resetRealtimeLastEventId() {
        _uiState.update { state -> state.copy(realtimeLastEventId = "") }
    }

    fun incrementRealtimeReconnectGeneration() {
        _uiState.update { state ->
            state.copy(realtimeReconnectGeneration = state.realtimeReconnectGeneration + 1)
        }
    }
}

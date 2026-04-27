package com.kzzz3.argus.lens.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.navigation.routeString
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.feature.auth.AuthStateHolder
import com.kzzz3.argus.lens.feature.auth.reduceAuthFormState
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.contacts.FriendRequestStatusState
import com.kzzz3.argus.lens.feature.inbox.ChatStateHolder
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxStateHolder
import com.kzzz3.argus.lens.feature.register.reduceRegisterFormState
import com.kzzz3.argus.lens.feature.wallet.WalletEffectHandler
import com.kzzz3.argus.lens.feature.wallet.WalletFeatureController
import com.kzzz3.argus.lens.feature.wallet.WalletRequestRunner
import com.kzzz3.argus.lens.feature.wallet.WalletStateHolder
import com.kzzz3.argus.lens.feature.wallet.reduceWalletState
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
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val runtimeScope: CoroutineScope = viewModelScope
    val authStateHolder: AuthStateHolder = createAuthStateHolder(dependencies, runtimeScope)
    val chatStateHolder: ChatStateHolder = createChatStateHolder()
    val inboxStateHolder: InboxStateHolder = createInboxStateHolder()
    val walletStateHolder: WalletStateHolder = createWalletStateHolder(dependencies, runtimeScope)
    private val savedRestorableEntryContext = savedStateHandle.readRestorableEntryContext()

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
            restorableEntryContext = savedRestorableEntryContext,
        )
    )
    val uiState: StateFlow<ArgusLensAppUiState> = _uiState.asStateFlow()

    fun openRoute(route: AppRoute) {
        _uiState.update { state ->
            val nextState = state.copy(
                currentRoute = route,
                restorableEntryContext = if (route == AppRoute.Chat) {
                    createRestorableChatEntryContext(state.appSessionState, state.selectedConversationId)
                } else {
                    null
                },
            )
            syncRestorableEntryContext(nextState.restorableEntryContext)
            nextState
        }
    }

    fun openConversation(conversationId: String) {
        _uiState.update { state ->
            val nextState = state.copy(
                currentRoute = AppRoute.Chat,
                selectedConversationId = conversationId,
                restorableEntryContext = createRestorableChatEntryContext(
                    session = state.appSessionState,
                    selectedConversationId = conversationId,
                ),
            )
            syncRestorableEntryContext(nextState.restorableEntryContext)
            nextState
        }
    }

    fun restoreSelectedConversation(conversationId: String) {
        _uiState.update { state ->
            val nextState = state.copy(
                selectedConversationId = conversationId,
                restorableEntryContext = createRestorableChatEntryContext(
                    session = state.appSessionState,
                    selectedConversationId = conversationId,
                ),
            )
            syncRestorableEntryContext(nextState.restorableEntryContext)
            nextState
        }
    }

    fun clearRestorableEntryContext() {
        _uiState.update { state ->
            val nextState = state.copy(
                selectedConversationId = "",
                restorableEntryContext = null,
            )
            syncRestorableEntryContext(null)
            nextState
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

    fun updateFriends(friends: List<FriendEntry>) {
        _uiState.update { state -> state.copy(friends = friends) }
    }

    fun updateConversationThreadsState(conversationThreadsState: ConversationThreadsState) {
        _uiState.update { state -> state.copy(conversationThreadsState = conversationThreadsState) }
    }

    fun applyHydratedSession(
        session: AppSessionState,
        hydratedConversationAccountId: String?,
    ) {
        _uiState.update { state ->
            val nextState = applyHydratedSessionTransition(state, session, hydratedConversationAccountId)
                .withSyncedRestorableEntryContext()
            syncRestorableEntryContext(nextState.restorableEntryContext)
            nextState
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
            val nextState = applyAuthenticatedSessionTransition(
                state = state,
                session = session,
                hydratedConversationAccountId = hydratedConversationAccountId,
                realtimeReconnectIncrement = realtimeReconnectIncrement,
            )
            syncRestorableEntryContext(null)
            nextState
        }
    }

    fun applyRefreshedSession(session: AppSessionState) {
        _uiState.update { state -> applyRefreshedSessionTransition(state, session) }
    }

    fun clearSession() {
        dependencies.sessionCredentialsStore.clear()
        _uiState.update { state ->
            val nextState = applySessionClearedTransition(state)
            syncRestorableEntryContext(null)
            nextState
        }
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

    private fun syncRestorableEntryContext(context: AppRestorableEntryContext?) {
        if (context == null) {
            savedStateHandle.remove<String>(RESTORABLE_ENTRY_ACCOUNT_ID_KEY)
            savedStateHandle.remove<String>(RESTORABLE_ENTRY_ROUTE_KEY)
            savedStateHandle.remove<String>(RESTORABLE_ENTRY_SELECTED_CONVERSATION_ID_KEY)
            return
        }

        savedStateHandle[RESTORABLE_ENTRY_ACCOUNT_ID_KEY] = context.accountId
        savedStateHandle[RESTORABLE_ENTRY_ROUTE_KEY] = context.routeString
        savedStateHandle[RESTORABLE_ENTRY_SELECTED_CONVERSATION_ID_KEY] = context.selectedConversationId
    }
}

private fun createChatStateHolder(): ChatStateHolder {
    return ChatStateHolder()
}

private fun createInboxStateHolder(): InboxStateHolder {
    return InboxStateHolder()
}

private fun createAuthStateHolder(
    dependencies: AppDependencies,
    runtimeScope: CoroutineScope,
): AuthStateHolder {
    val authCoordinator = dependencies.authCoordinator
    return AuthStateHolder(
        scope = runtimeScope,
        reduceAuthAction = ::reduceAuthFormState,
        reduceRegisterAction = ::reduceRegisterFormState,
        login = authCoordinator::login,
        register = authCoordinator::register,
    )
}

private fun createWalletStateHolder(
    dependencies: AppDependencies,
    runtimeScope: CoroutineScope,
): WalletStateHolder {
    val walletRequestRunner = WalletRequestRunner(runtimeScope)
    val walletRequestCoordinator = dependencies.walletRequestCoordinator
    val walletEffectHandler = WalletEffectHandler(
        requestRunner = walletRequestRunner,
        loadWalletSummary = walletRequestCoordinator::loadWalletSummary,
        resolvePayload = walletRequestCoordinator::resolvePayload,
        confirmPayment = walletRequestCoordinator::confirmPayment,
        loadPaymentHistory = walletRequestCoordinator::loadPaymentHistory,
        loadPaymentReceipt = walletRequestCoordinator::loadPaymentReceipt,
    )
    val walletFeatureController = WalletFeatureController(
        reduceAction = ::reduceWalletState,
        effectHandler = walletEffectHandler,
    )
    return WalletStateHolder(
        controller = walletFeatureController,
        invalidateRequests = walletRequestRunner::invalidate,
    )
}

private const val RESTORABLE_ENTRY_ACCOUNT_ID_KEY = "restorableEntryAccountId"
private const val RESTORABLE_ENTRY_ROUTE_KEY = "restorableEntryRoute"
private const val RESTORABLE_ENTRY_SELECTED_CONVERSATION_ID_KEY = "restorableEntrySelectedConversationId"

private fun SavedStateHandle.readRestorableEntryContext(): AppRestorableEntryContext? {
    val accountId = get<String>(RESTORABLE_ENTRY_ACCOUNT_ID_KEY).orEmpty()
    val routeString = get<String>(RESTORABLE_ENTRY_ROUTE_KEY).orEmpty()
    val selectedConversationId = get<String>(RESTORABLE_ENTRY_SELECTED_CONVERSATION_ID_KEY).orEmpty()
    return AppRestorableEntryContext(
        accountId = accountId,
        routeString = routeString,
        selectedConversationId = selectedConversationId,
    ).takeIf {
        it.accountId.isNotBlank() &&
            it.routeString.isNotBlank() &&
            it.selectedConversationId.isNotBlank()
    }
}

private fun createRestorableChatEntryContext(
    session: AppSessionState,
    selectedConversationId: String,
): AppRestorableEntryContext? {
    return AppRestorableEntryContext(
        accountId = session.accountId,
        routeString = AppRoute.Chat.routeString,
        selectedConversationId = selectedConversationId,
    ).takeIf {
        session.isAuthenticated &&
            session.accountId.isNotBlank() &&
            selectedConversationId.isNotBlank()
    }
}

private fun ArgusLensAppUiState.withSyncedRestorableEntryContext(): ArgusLensAppUiState {
    val context = if (currentRoute == AppRoute.Chat) {
        createRestorableChatEntryContext(appSessionState, selectedConversationId)
    } else {
        null
    }
    return copy(restorableEntryContext = context)
}

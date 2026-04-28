package com.kzzz3.argus.lens.app.state

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kzzz3.argus.lens.app.composition.AppDependencies
import com.kzzz3.argus.lens.app.host.AppFeatureStateHolders
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.navigation.routeString
import com.kzzz3.argus.lens.app.runtime.AppRestorableEntryContext
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.feature.auth.AuthStateHolder
import com.kzzz3.argus.lens.feature.auth.reduceAuthFormState
import com.kzzz3.argus.lens.feature.call.CallSessionStateHolder
import com.kzzz3.argus.lens.feature.contacts.ContactsFeatureStateHolder
import com.kzzz3.argus.lens.feature.inbox.ChatStateHolder
import com.kzzz3.argus.lens.feature.inbox.InboxChatFeatureState
import com.kzzz3.argus.lens.feature.inbox.InboxChatFeatureStateHolder
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
    val appScope: CoroutineScope = viewModelScope
    private val initialConversationThreadsState = dependencies.appShellUseCases.createPreviewConversationThreads(
        currentUserDisplayName = DEFAULT_PREVIEW_DISPLAY_NAME,
    )
    internal val featureStateHolders: AppFeatureStateHolders = AppFeatureStateHolders(
        authStateHolder = createAuthStateHolder(dependencies, appScope),
        contactsFeatureStateHolder = ContactsFeatureStateHolder(),
        callSessionStateHolder = CallSessionStateHolder(),
        inboxChatFeatureStateHolder = InboxChatFeatureStateHolder(
            InboxChatFeatureState(conversationThreadsState = initialConversationThreadsState)
        ),
        chatStateHolder = createChatStateHolder(),
        inboxStateHolder = createInboxStateHolder(),
        walletStateHolder = createWalletStateHolder(dependencies, appScope),
    )
    private val savedRestorableEntryContext = savedStateHandle.readRestorableEntryContext()

    private val _uiState = MutableStateFlow(
        ArgusLensAppUiState(
            appSessionState = dependencies.initialSessionSnapshot,
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
                    createRestorableChatEntryContext(state.appSessionState, state.activeChatConversationId)
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
                activeChatConversationId = conversationId,
                restorableEntryContext = createRestorableChatEntryContext(
                    session = state.appSessionState,
                    activeChatConversationId = conversationId,
                ),
            )
            syncRestorableEntryContext(nextState.restorableEntryContext)
            nextState
        }
    }

    fun restoreActiveChatConversation(conversationId: String) {
        _uiState.update { state ->
            val nextState = state.copy(
                activeChatConversationId = conversationId,
                restorableEntryContext = createRestorableChatEntryContext(
                    session = state.appSessionState,
                    activeChatConversationId = conversationId,
                ),
            )
            syncRestorableEntryContext(nextState.restorableEntryContext)
            nextState
        }
    }

    fun clearRestorableEntryContext() {
        _uiState.update { state ->
            val nextState = state.copy(
                activeChatConversationId = "",
                restorableEntryContext = null,
            )
            syncRestorableEntryContext(null)
            nextState
        }
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
            savedStateHandle.remove<String>(RESTORABLE_CHAT_CONVERSATION_ID_KEY)
            return
        }

        savedStateHandle[RESTORABLE_ENTRY_ACCOUNT_ID_KEY] = context.accountId
        savedStateHandle[RESTORABLE_ENTRY_ROUTE_KEY] = context.routeString
        savedStateHandle[RESTORABLE_CHAT_CONVERSATION_ID_KEY] = context.activeChatConversationId
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
    appScope: CoroutineScope,
): AuthStateHolder {
    val authUseCases = dependencies.authUseCases
    return AuthStateHolder(
        scope = appScope,
        reduceAuthAction = ::reduceAuthFormState,
        reduceRegisterAction = ::reduceRegisterFormState,
        login = authUseCases::login,
        register = authUseCases::register,
    )
}

private fun createWalletStateHolder(
    dependencies: AppDependencies,
    appScope: CoroutineScope,
): WalletStateHolder {
    val walletRequestRunner = WalletRequestRunner(appScope)
    val walletUseCases = dependencies.walletUseCases
    val walletEffectHandler = WalletEffectHandler(
        requestRunner = walletRequestRunner,
        loadWalletSummary = walletUseCases::loadWalletSummary,
        resolvePayload = walletUseCases::resolvePayload,
        confirmPayment = walletUseCases::confirmPayment,
        loadPaymentHistory = walletUseCases::loadPaymentHistory,
        loadPaymentReceipt = walletUseCases::loadPaymentReceipt,
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
private const val RESTORABLE_CHAT_CONVERSATION_ID_KEY = "restorableChatConversationId"

private fun SavedStateHandle.readRestorableEntryContext(): AppRestorableEntryContext? {
    val accountId = get<String>(RESTORABLE_ENTRY_ACCOUNT_ID_KEY).orEmpty()
    val routeString = get<String>(RESTORABLE_ENTRY_ROUTE_KEY).orEmpty()
    val activeChatConversationId = get<String>(RESTORABLE_CHAT_CONVERSATION_ID_KEY).orEmpty()
    return AppRestorableEntryContext(
        accountId = accountId,
        routeString = routeString,
        activeChatConversationId = activeChatConversationId,
    ).takeIf {
        it.accountId.isNotBlank() &&
            it.routeString.isNotBlank() &&
            it.activeChatConversationId.isNotBlank()
    }
}

private fun createRestorableChatEntryContext(
    session: AppSessionState,
    activeChatConversationId: String,
): AppRestorableEntryContext? {
    return AppRestorableEntryContext(
        accountId = session.accountId,
        routeString = AppRoute.Chat.routeString,
        activeChatConversationId = activeChatConversationId,
    ).takeIf {
        session.isAuthenticated &&
            session.accountId.isNotBlank() &&
            activeChatConversationId.isNotBlank()
    }
}

private fun ArgusLensAppUiState.withSyncedRestorableEntryContext(): ArgusLensAppUiState {
    val context = if (currentRoute == AppRoute.Chat) {
        createRestorableChatEntryContext(appSessionState, activeChatConversationId)
    } else {
        null
    }
    return copy(restorableEntryContext = context)
}

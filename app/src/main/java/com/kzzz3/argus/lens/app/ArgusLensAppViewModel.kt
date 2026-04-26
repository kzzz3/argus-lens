package com.kzzz3.argus.lens.app

import androidx.lifecycle.ViewModel
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.model.session.AppSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ArgusLensAppViewModel @Inject constructor(
    val dependencies: AppDependencies,
) : ViewModel() {
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

    fun clearSelectedConversation() {
        _uiState.update { state -> state.copy(selectedConversationId = "") }
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

    fun incrementRealtimeReconnectGenerationBy(amount: Int) {
        if (amount == 0) return
        _uiState.update { state ->
            state.copy(realtimeReconnectGeneration = state.realtimeReconnectGeneration + amount)
        }
    }
}

data class ArgusLensAppUiState(
    val appSessionState: AppSessionState,
    val currentRoute: AppRoute,
    val selectedConversationId: String = "",
    val hydratedConversationAccountId: String? = null,
    val realtimeConnectionState: ConversationRealtimeConnectionState = ConversationRealtimeConnectionState.DISABLED,
    val realtimeLastEventId: String = "",
    val realtimeReconnectGeneration: Int = 0,
)

internal fun resolveInitialAppRoute(
    session: AppSessionState,
    credentials: SessionCredentials,
): AppRoute {
    return if (session.isAuthenticated && credentials.hasAccessToken) {
        AppRoute.Inbox
    } else {
        AppRoute.AuthEntry
    }
}

internal fun resolveInitialHydratedConversationAccountId(session: AppSessionState): String? {
    return if (session.isAuthenticated) {
        session.accountId.takeIf { it.isNotBlank() }
    } else {
        null
    }
}

internal fun applyHydratedSessionTransition(
    state: ArgusLensAppUiState,
    session: AppSessionState,
    hydratedConversationAccountId: String?,
): ArgusLensAppUiState {
    return state.copy(
        appSessionState = session,
        currentRoute = if (session.isAuthenticated) AppRoute.Inbox else state.currentRoute,
        hydratedConversationAccountId = hydratedConversationAccountId,
    )
}

internal fun applyAuthenticatedSessionTransition(
    state: ArgusLensAppUiState,
    session: AppSessionState,
    hydratedConversationAccountId: String,
    realtimeReconnectIncrement: Int,
): ArgusLensAppUiState {
    return state.copy(
        appSessionState = session,
        currentRoute = AppRoute.Inbox,
        selectedConversationId = "",
        hydratedConversationAccountId = hydratedConversationAccountId,
        realtimeReconnectGeneration = state.realtimeReconnectGeneration + realtimeReconnectIncrement,
    )
}

internal fun applyRefreshedSessionTransition(
    state: ArgusLensAppUiState,
    session: AppSessionState,
): ArgusLensAppUiState {
    return state.copy(appSessionState = session)
}

internal fun applySessionClearedTransition(state: ArgusLensAppUiState): ArgusLensAppUiState {
    return state.copy(
        appSessionState = AppSessionState(),
        currentRoute = AppRoute.AuthEntry,
        selectedConversationId = "",
        hydratedConversationAccountId = null,
    )
}

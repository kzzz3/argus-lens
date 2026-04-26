package com.kzzz3.argus.lens.app

import androidx.lifecycle.ViewModel
import com.kzzz3.argus.lens.app.navigation.AppRoute
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

    fun updateHydratedConversationAccountId(accountId: String?) {
        _uiState.update { state -> state.copy(hydratedConversationAccountId = accountId) }
    }
}

data class ArgusLensAppUiState(
    val currentRoute: AppRoute,
    val selectedConversationId: String = "",
    val hydratedConversationAccountId: String? = null,
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

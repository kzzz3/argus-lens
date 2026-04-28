package com.kzzz3.argus.lens.app.state

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.runtime.AppRestorableEntryContext
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.model.session.AppSessionState

data class ArgusLensAppUiState(
    val appSessionState: AppSessionState,
    val currentRoute: AppRoute,
    val activeChatConversationId: String = "",
    val restorableEntryContext: AppRestorableEntryContext? = null,
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
        activeChatConversationId = "",
        restorableEntryContext = null,
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
        activeChatConversationId = "",
        restorableEntryContext = null,
        hydratedConversationAccountId = null,
    )
}

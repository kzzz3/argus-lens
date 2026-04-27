package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.model.session.AppSessionState

data class ArgusLensAppUiState(
    val appSessionState: AppSessionState,
    val conversationThreadsState: ConversationThreadsState = ConversationThreadsState(),
    val currentRoute: AppRoute,
    val authFormState: AuthFormState = AuthFormState(),
    val registerFormState: RegisterFormState = RegisterFormState(),
    val callSessionState: CallSessionState = CallSessionState(),
    val contactsState: ContactsState = ContactsState(),
    val walletState: WalletState = WalletState(),
    val friends: List<FriendEntry> = emptyList(),
    val selectedConversationId: String = "",
    val chatStatusMessage: String? = null,
    val chatStatusError: Boolean = false,
    val friendRequestsSnapshot: FriendRequestsSnapshot = FriendRequestsSnapshot(emptyList(), emptyList()),
    val friendRequestsStatusMessage: String? = null,
    val friendRequestsStatusError: Boolean = false,
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

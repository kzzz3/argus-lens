package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.contacts.FriendRequestStatusState
import com.kzzz3.argus.lens.feature.contacts.reduceContactsState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.wallet.reduceWalletState
import com.kzzz3.argus.lens.ui.shell.ShellDestination

internal class AppRouteActionBindings(
    private val state: AppRouteHostState,
    private val callbacks: AppRouteHostCallbacks,
    private val routeUiState: AppRouteUiState,
    private val routeRuntimes: AppRouteRuntimes,
    private val previewThreadsState: ConversationThreadsState,
    private val sessionBoundaryRuntime: AppSessionBoundaryRuntime,
    private val sessionBoundaryCallbacks: AppSessionBoundaryCallbacks,
    private val getLatestCurrentRoute: () -> AppRoute,
) {
    private val contactsActionRouteRuntime = ContactsActionRouteRuntime(
        reduceAction = ::reduceContactsState,
        handleEffect = { effect ->
            routeRuntimes.contactsRouteRuntime.handleContactsEffect(
                effect = effect,
                request = contactsRouteRequest(),
                callbacks = contactsRouteCallbacks(),
            )
        },
    )

    private val walletActionRouteRuntime = WalletActionRouteRuntime(
        reduceAction = ::reduceWalletState,
        handleEffect = { effect, currentState ->
            routeRuntimes.walletRouteRuntime.handleEffect(
                effect = effect,
                request = WalletRouteRequest(
                    session = state.appSessionState,
                    currentState = currentState,
                ),
                callbacks = WalletRouteCallbacks(
                    getCurrentSession = { state.appSessionState },
                    getCurrentState = { state.walletStateModel },
                    onRouteChanged = ::openTopLevelRoute,
                    onStateChanged = callbacks.onWalletStateChanged,
                ),
            )
        },
    )

    fun openTopLevelRoute(route: AppRoute) {
        routeRuntimes.appRouteNavigationRuntime.openTopLevelRoute(
            route = route,
            request = AppRouteNavigationRequest(
                accountId = state.appSessionState.accountId,
                walletState = state.walletStateModel,
            ),
            callbacks = AppRouteNavigationCallbacks(
                onWalletStateChanged = callbacks.onWalletStateChanged,
                onRouteChanged = callbacks.onRouteChanged,
            ),
        )
    }

    fun openShellDestination(destination: ShellDestination) {
        routeRuntimes.appRouteNavigationRuntime.openShellDestination(
            destination = destination,
            request = AppRouteNavigationRequest(
                accountId = state.appSessionState.accountId,
                walletState = state.walletStateModel,
            ),
            callbacks = AppRouteNavigationCallbacks(
                onWalletStateChanged = callbacks.onWalletStateChanged,
                onRouteChanged = callbacks.onRouteChanged,
            ),
        )
    }

    fun openInboxConversation(conversationId: String) {
        routeRuntimes.inboxRouteRuntime.openConversation(
            conversationId = conversationId,
            request = InboxRouteRequest(threadsState = state.conversationThreadsState),
            callbacks = InboxRouteCallbacks(
                onConversationThreadsChanged = callbacks.onConversationThreadsChanged,
                onConversationOpened = callbacks.onConversationOpened,
            ),
        )
    }

    suspend fun applySuccessfulAuthResult(
        authResult: AuthRepositoryResult.Success,
        keepSubmitMessageOnAuthForm: Boolean,
    ) {
        sessionBoundaryRuntime.applySuccessfulAuthResult(
            authResult = authResult,
            keepSubmitMessageOnAuthForm = keepSubmitMessageOnAuthForm,
            callbacks = sessionBoundaryCallbacks,
        )
    }

    fun applyFriendRequestStatus(statusState: FriendRequestStatusState) {
        callbacks.onFriendRequestStatusChanged(statusState)
    }

    fun entryRouteRequest(): EntryRouteRequest {
        return EntryRouteRequest(
            authFormState = state.authFormState,
            registerFormState = state.registerFormState,
        )
    }

    fun entryRouteCallbacks(): EntryRouteCallbacks {
        return EntryRouteCallbacks(
            onRouteChanged = callbacks.onRouteChanged,
            onAuthFormStateChanged = callbacks.onAuthFormStateChanged,
            onRegisterFormStateChanged = callbacks.onRegisterFormStateChanged,
            applySuccessfulAuthResult = ::applySuccessfulAuthResult,
        )
    }

    fun contactsRouteRequest(contactsStateSnapshot: ContactsState = state.contactsState): ContactsRouteRequest {
        return ContactsRouteRequest(
            session = state.appSessionState,
            contactsState = contactsStateSnapshot,
            friends = state.friends,
            conversationThreadsState = state.conversationThreadsState,
            friendRequestsSnapshot = state.friendRequestsSnapshot,
        )
    }

    fun contactsRouteCallbacks(): ContactsRouteCallbacks {
        return ContactsRouteCallbacks(
            onRouteChanged = callbacks.onRouteChanged,
            onContactsStateChanged = callbacks.onContactsStateChanged,
            onFriendRequestsSnapshotChanged = callbacks.onFriendRequestsSnapshotChanged,
            onConversationThreadsChanged = callbacks.onConversationThreadsChanged,
            onConversationOpened = callbacks.onConversationOpened,
            onFriendRequestStatusChanged = ::applyFriendRequestStatus,
            onFriendsChanged = callbacks.onFriendsChanged,
        )
    }

    fun signOutToEntry(message: String? = null) {
        sessionBoundaryRuntime.signOutToEntry(
            currentSession = state.appSessionState,
            previewThreadsState = previewThreadsState,
            message = message,
            callbacks = sessionBoundaryCallbacks,
        )
    }

    fun inboxActionRouteCallbacks(): InboxActionRouteCallbacks {
        return InboxActionRouteCallbacks(
            openConversation = ::openInboxConversation,
            openTopLevelRoute = ::openTopLevelRoute,
            signOutToEntry = { signOutToEntry() },
        )
    }

    suspend fun refreshSessionTokens(): AuthRepositoryResult {
        return sessionBoundaryRuntime.refreshSessionTokens(
            session = state.appSessionState,
            setSession = callbacks.onSessionRefreshed,
        )
    }

    fun scheduleSessionRefreshLoop() {
        sessionBoundaryRuntime.scheduleSessionRefreshLoop(
            onUnauthorized = { signOutToEntry("Session expired or was revoked. Please sign in again.") },
        )
    }

    fun realtimeConnectionCallbacks(): RealtimeConnectionCallbacks {
        return RealtimeConnectionCallbacks(
            onConnectionStateChanged = callbacks.onRealtimeConnectionStateChanged,
            onEventIdRecorded = callbacks.onRealtimeEventIdRecorded,
            onLastEventIdReset = callbacks.onRealtimeLastEventIdReset,
            onConversationThreadsChanged = callbacks.onConversationThreadsChanged,
            onReconnectGenerationIncremented = callbacks.onRealtimeReconnectIncremented,
            onScheduleSessionRefreshLoop = { scheduleSessionRefreshLoop() },
            onCancelSessionRefreshLoop = routeRuntimes.sessionRefreshRuntime::cancel,
            refreshSessionTokens = { refreshSessionTokens() },
            signOutToEntry = { message -> signOutToEntry(message) },
        )
    }

    fun handleAuthAction(action: com.kzzz3.argus.lens.feature.auth.AuthEntryAction) {
        routeRuntimes.entryRouteRuntime.handleAuthAction(
            action = action,
            request = entryRouteRequest(),
            callbacks = entryRouteCallbacks(),
        )
    }

    fun handleRegisterAction(action: com.kzzz3.argus.lens.feature.register.RegisterAction) {
        routeRuntimes.entryRouteRuntime.handleRegisterAction(
            action = action,
            request = entryRouteRequest(),
            callbacks = entryRouteCallbacks(),
        )
    }

    fun handleInboxAction(action: com.kzzz3.argus.lens.feature.inbox.InboxAction) {
        routeRuntimes.inboxActionRouteRuntime.handleAction(action, inboxActionRouteCallbacks())
    }

    fun handleContactsAction(action: com.kzzz3.argus.lens.feature.contacts.ContactsAction) {
        contactsActionRouteRuntime.handleAction(
            action = action,
            request = ContactsActionRouteRequest(currentState = state.contactsState),
            callbacks = ContactsActionRouteCallbacks(
                onContactsStateChanged = callbacks.onContactsStateChanged,
            ),
        )
    }

    fun handleNewFriendsAction(action: com.kzzz3.argus.lens.feature.contacts.NewFriendsAction) {
        routeRuntimes.contactsRouteRuntime.handleNewFriendsAction(
            action = action,
            request = contactsRouteRequest(),
            callbacks = contactsRouteCallbacks(),
        )
    }

    fun handleCallSessionAction(action: com.kzzz3.argus.lens.feature.call.CallSessionAction) {
        routeRuntimes.callSessionRouteRuntime.handleAction(
            action = action,
            request = CallSessionRouteRequest(currentState = state.callSessionState),
            callbacks = CallSessionRouteCallbacks(
                onCallSessionStateChanged = callbacks.onCallSessionStateChanged,
                onRouteChanged = callbacks.onRouteChanged,
            ),
        )
    }

    fun handleWalletAction(action: com.kzzz3.argus.lens.feature.wallet.WalletAction) {
        walletActionRouteRuntime.handleAction(
            action = action,
            request = WalletActionRouteRequest(currentState = state.walletStateModel),
            callbacks = WalletActionRouteCallbacks(
                onWalletStateChanged = callbacks.onWalletStateChanged,
            ),
        )
    }

    fun handleChatAction(action: com.kzzz3.argus.lens.feature.inbox.ChatAction) {
        routeUiState.chatState?.let { resolvedChatState ->
            routeRuntimes.chatRouteRuntime.handleAction(
                action = action,
                request = ChatRouteRequest(
                    threadsState = state.conversationThreadsState,
                    chatState = resolvedChatState,
                ),
                callbacks = ChatRouteCallbacks(
                    onRouteChanged = ::openTopLevelRoute,
                    onConversationThreadsChanged = callbacks.onConversationThreadsChanged,
                    onChatStatusChanged = callbacks.onChatStatusChanged,
                    onCallSessionStateChanged = callbacks.onCallSessionStateChanged,
                    getCurrentRoute = getLatestCurrentRoute,
                ),
            )
        }
    }
}

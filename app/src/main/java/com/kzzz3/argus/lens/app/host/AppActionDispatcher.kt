package com.kzzz3.argus.lens.app.host

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.runtime.AppRouteNavigationCallbacks
import com.kzzz3.argus.lens.app.runtime.AppRouteNavigationRequest
import com.kzzz3.argus.lens.app.runtime.AppSessionBoundaryCallbacks
import com.kzzz3.argus.lens.app.runtime.SessionBoundaryHandler
import com.kzzz3.argus.lens.core.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.feature.auth.AuthStateHolder
import com.kzzz3.argus.lens.feature.auth.AuthStateHolderCallbacks
import com.kzzz3.argus.lens.feature.call.CallSessionFeatureCallbacks
import com.kzzz3.argus.lens.feature.call.CallSessionFeatureSnapshot
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.contacts.ContactsFeatureCallbacks
import com.kzzz3.argus.lens.feature.contacts.ContactsFeatureSnapshot
import com.kzzz3.argus.lens.feature.contacts.ContactsFeatureState
import com.kzzz3.argus.lens.feature.contacts.FriendRequestStatusState
import com.kzzz3.argus.lens.feature.inbox.InboxChatFeatureCallbacks
import com.kzzz3.argus.lens.feature.inbox.InboxChatFeatureController
import com.kzzz3.argus.lens.feature.inbox.InboxChatFeatureSnapshot
import com.kzzz3.argus.lens.feature.inbox.InboxChatFeatureState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.realtime.RealtimeConnectionCallbacks
import com.kzzz3.argus.lens.feature.wallet.WalletStateHolder
import com.kzzz3.argus.lens.session.SessionRefreshOutcome
import com.kzzz3.argus.lens.ui.shell.ShellDestination

internal class AppActionDispatcher(
    private val state: AppShellState,
    private val contactsFeatureState: ContactsFeatureState,
    private val callSessionState: CallSessionState,
    private val inboxChatFeatureState: InboxChatFeatureState,
    private val authStateHolder: AuthStateHolder,
    private val inboxChatFeatureController: InboxChatFeatureController,
    private val walletStateHolder: WalletStateHolder,
    private val callbacks: AppShellCallbacks,
    private val featureCallbacks: AppFeatureCallbacks,
    private val routeUiState: AppShellUiState,
    private val routeHandlers: AppRouteHandlers,
    private val previewThreadsState: ConversationThreadsState,
    private val sessionBoundaryHandler: SessionBoundaryHandler,
    private val sessionBoundaryCallbacks: AppSessionBoundaryCallbacks,
    private val getLatestCurrentRoute: () -> AppRoute,
) {
    fun openTopLevelRoute(route: AppRoute) {
        routeHandlers.appRouteNavigator.openTopLevelRoute(
            route = route,
            request = AppRouteNavigationRequest(
                accountId = state.appSessionState.accountId,
            ),
            callbacks = AppRouteNavigationCallbacks(
                onWalletOpened = walletStateHolder::openForAccount,
                onRouteChanged = callbacks.onRouteChanged,
            ),
        )
    }

    fun openShellDestination(destination: ShellDestination) {
        routeHandlers.appRouteNavigator.openShellDestination(
            destination = destination,
            request = AppRouteNavigationRequest(
                accountId = state.appSessionState.accountId,
            ),
            callbacks = AppRouteNavigationCallbacks(
                onWalletOpened = walletStateHolder::openForAccount,
                onRouteChanged = callbacks.onRouteChanged,
            ),
        )
    }

    suspend fun applySuccessfulAuthResult(
        authResult: AuthRepositoryResult.Success,
        keepSubmitMessageOnAuthForm: Boolean,
    ) {
        sessionBoundaryHandler.applySuccessfulAuthResult(
            authResult = authResult,
            keepSubmitMessageOnAuthForm = keepSubmitMessageOnAuthForm,
            callbacks = sessionBoundaryCallbacks,
        )
    }

    fun applyFriendRequestStatus(statusState: FriendRequestStatusState) {
        featureCallbacks.onFriendRequestStatusChanged(statusState)
    }

    fun authStateHolderCallbacks(): AuthStateHolderCallbacks {
        return AuthStateHolderCallbacks(
            onNavigateToRegister = { callbacks.onRouteChanged(AppRoute.RegisterEntry) },
            onNavigateBackToLogin = { callbacks.onRouteChanged(AppRoute.AuthEntry) },
            applySuccessfulAuthResult = ::applySuccessfulAuthResult,
        )
    }

    fun contactsFeatureSnapshot(): ContactsFeatureSnapshot {
        return ContactsFeatureSnapshot(
            session = state.appSessionState,
            contactsState = contactsFeatureState.contactsState,
            friends = contactsFeatureState.friends,
            conversationThreadsState = inboxChatFeatureState.conversationThreadsState,
            friendRequestsSnapshot = contactsFeatureState.friendRequestsSnapshot,
        )
    }

    fun contactsFeatureCallbacks(): ContactsFeatureCallbacks {
        return ContactsFeatureCallbacks(
            onOpenNewFriends = { callbacks.onRouteChanged(AppRoute.NewFriends) },
            onNavigateBackToInbox = { callbacks.onRouteChanged(AppRoute.Inbox) },
            onNavigateBackToContacts = { callbacks.onRouteChanged(AppRoute.Contacts) },
            onContactsStateChanged = featureCallbacks.onContactsStateChanged,
            onFriendRequestsSnapshotChanged = featureCallbacks.onFriendRequestsSnapshotChanged,
            onConversationThreadsChanged = featureCallbacks.onConversationThreadsChanged,
            onConversationOpened = callbacks.onConversationOpened,
            onFriendRequestStatusChanged = ::applyFriendRequestStatus,
            onFriendsChanged = featureCallbacks.onFriendsChanged,
        )
    }

    fun signOutToEntry(message: String? = null) {
        sessionBoundaryHandler.signOutToEntry(
            currentSession = state.appSessionState,
            previewThreadsState = previewThreadsState,
            message = message,
            callbacks = sessionBoundaryCallbacks,
        )
    }

    suspend fun refreshSessionTokens(): SessionRefreshOutcome {
        return sessionBoundaryHandler.refreshSessionTokens(
            session = state.appSessionState,
            setSession = callbacks.onSessionRefreshed,
        )
    }

    fun scheduleSessionRefreshLoop() {
        sessionBoundaryHandler.scheduleSessionRefreshLoop(
            onUnauthorized = { signOutToEntry("Session expired or was revoked. Please sign in again.") },
        )
    }

    fun realtimeConnectionCallbacks(): RealtimeConnectionCallbacks {
        return RealtimeConnectionCallbacks(
            onConnectionStateChanged = callbacks.onRealtimeConnectionStateChanged,
            onEventIdRecorded = callbacks.onRealtimeEventIdRecorded,
            onLastEventIdReset = callbacks.onRealtimeLastEventIdReset,
            onConversationThreadsChanged = featureCallbacks.onConversationThreadsChanged,
            onReconnectGenerationIncremented = callbacks.onRealtimeReconnectIncremented,
            onScheduleSessionRefreshLoop = { scheduleSessionRefreshLoop() },
            onCancelSessionRefreshLoop = routeHandlers.sessionRefreshScheduler::cancel,
            refreshSessionTokens = { refreshSessionTokens() },
            signOutToEntry = { message -> signOutToEntry(message) },
        )
    }

    fun handleAuthAction(action: com.kzzz3.argus.lens.feature.auth.AuthEntryAction) {
        authStateHolder.handleAuthAction(
            action = action,
            callbacks = authStateHolderCallbacks(),
        )
    }

    fun handleRegisterAction(action: com.kzzz3.argus.lens.feature.register.RegisterAction) {
        authStateHolder.handleRegisterAction(
            action = action,
            callbacks = authStateHolderCallbacks(),
        )
    }

    fun handleInboxAction(action: com.kzzz3.argus.lens.feature.inbox.InboxAction) {
        inboxChatFeatureController.handleInboxAction(
            action = action,
            snapshot = inboxChatFeatureSnapshot(),
            callbacks = inboxChatFeatureCallbacks(),
        )
    }

    fun handleContactsAction(action: com.kzzz3.argus.lens.feature.contacts.ContactsAction) {
        routeHandlers.contactsFeatureController.handleContactsAction(
            action = action,
            snapshot = contactsFeatureSnapshot(),
            callbacks = contactsFeatureCallbacks(),
        )
    }

    fun handleNewFriendsAction(action: com.kzzz3.argus.lens.feature.contacts.NewFriendsAction) {
        routeHandlers.contactsFeatureController.handleNewFriendsAction(
            action = action,
            snapshot = contactsFeatureSnapshot(),
            callbacks = contactsFeatureCallbacks(),
        )
    }

    fun handleCallSessionAction(action: com.kzzz3.argus.lens.feature.call.CallSessionAction) {
        routeHandlers.callSessionFeatureController.handleAction(
            action = action,
            snapshot = CallSessionFeatureSnapshot(currentState = callSessionState),
            callbacks = CallSessionFeatureCallbacks(
                onCallSessionStateChanged = featureCallbacks.onCallSessionStateChanged,
                onNavigateBackToChat = { callbacks.onRouteChanged(AppRoute.Chat) },
            ),
        )
    }

    fun handleWalletAction(action: com.kzzz3.argus.lens.feature.wallet.WalletAction) {
        walletStateHolder.handleAction(
            action = action,
            session = state.appSessionState,
            getCurrentSession = { state.appSessionState },
            onNavigateBackToInbox = { openTopLevelRoute(AppRoute.Inbox) },
        )
    }

    fun handleChatAction(action: com.kzzz3.argus.lens.feature.inbox.ChatAction) {
        inboxChatFeatureController.handleChatAction(
            action = action,
            snapshot = inboxChatFeatureSnapshot(),
            callbacks = inboxChatFeatureCallbacks(),
        )
    }

    private fun inboxChatFeatureSnapshot(): InboxChatFeatureSnapshot {
        return InboxChatFeatureSnapshot(
            threadsState = inboxChatFeatureState.conversationThreadsState,
            chatState = routeUiState.chatState,
        )
    }

    private fun inboxChatFeatureCallbacks(): InboxChatFeatureCallbacks {
        return InboxChatFeatureCallbacks(
            onOpenContacts = { openTopLevelRoute(AppRoute.Contacts) },
            onOpenWallet = { openTopLevelRoute(AppRoute.Wallet) },
            onSignOutToHud = { signOutToEntry() },
            onConversationThreadsChanged = featureCallbacks.onConversationThreadsChanged,
            onConversationOpened = callbacks.onConversationOpened,
            onNavigateBackToInbox = { openTopLevelRoute(AppRoute.Inbox) },
            onChatStatusChanged = featureCallbacks.onChatStatusChanged,
            onCallSessionStateChanged = featureCallbacks.onCallSessionStateChanged,
            onNavigateToCallSession = { callbacks.onRouteChanged(AppRoute.CallSession) },
            isCallSessionRouteActive = { getLatestCurrentRoute() == AppRoute.CallSession },
        )
    }
}

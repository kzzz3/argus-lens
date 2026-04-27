package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.kzzz3.argus.lens.feature.auth.AuthStateHolder
import com.kzzz3.argus.lens.feature.wallet.WalletStateHolder
import com.kzzz3.argus.lens.navigation.ArgusNavHost
import com.kzzz3.argus.lens.navigation.graphRouteForAppRoute
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun AppRouteHost(
    dependencies: AppDependencies,
    runtimeScope: CoroutineScope,
    authStateHolder: AuthStateHolder,
    walletStateHolder: WalletStateHolder,
    state: AppRouteHostState,
    callbacks: AppRouteHostCallbacks,
) {
    val appSessionState = state.appSessionState
    val conversationThreadsState = state.conversationThreadsState
    val currentRoute = state.currentRoute
    val callSessionState = state.callSessionState
    val contactsState = state.contactsState
    val friends = state.friends
    val selectedConversationId = state.selectedConversationId
    val chatStatusMessage = state.chatStatusMessage
    val chatStatusError = state.chatStatusError
    val friendRequestsSnapshot = state.friendRequestsSnapshot
    val friendRequestsStatusMessage = state.friendRequestsStatusMessage
    val friendRequestsStatusError = state.friendRequestsStatusError
    val realtimeConnectionState = state.realtimeConnectionState
    val onCallSessionStateChanged = callbacks.onCallSessionStateChanged
    val onContactsStateChanged = callbacks.onContactsStateChanged
    val onFriendsChanged = callbacks.onFriendsChanged
    val onFriendRequestStatusReset = callbacks.onFriendRequestStatusReset
    val onHydratedSessionApplied = callbacks.onHydratedSessionApplied
    val onAuthenticatedSessionApplied = callbacks.onAuthenticatedSessionApplied
    val onSessionRefreshed = callbacks.onSessionRefreshed
    val onSessionCleared = callbacks.onSessionCleared
    val onConversationThreadsChanged = callbacks.onConversationThreadsChanged
    val onHydratedConversationAccountChanged = callbacks.onHydratedConversationAccountChanged
    val navController = rememberNavController()
    val routeRuntimes = rememberAppRouteRuntimes(dependencies, runtimeScope)
    val appShellCoordinator = dependencies.appShellCoordinator
    val sessionCredentialsStore = dependencies.sessionCredentialsStore
    val callSessionRuntime = routeRuntimes.callSessionRuntime
    val sessionRefreshRuntime = routeRuntimes.sessionRefreshRuntime
    val authStateModel by authStateHolder.state.collectAsStateWithLifecycle()
    val walletStateModel by walletStateHolder.state.collectAsStateWithLifecycle()
    val appRouteNavigationRuntime = routeRuntimes.appRouteNavigationRuntime
    val previewThreadsState = remember {
        appShellCoordinator.createPreviewConversationThreads(
            currentUserDisplayName = DEFAULT_PREVIEW_DISPLAY_NAME,
        )
    }
    val startDestination = remember { graphRouteForAppRoute(currentRoute) }
    val routeUiState = rememberAppRouteUiState(
        appSessionState = appSessionState,
        conversationThreadsState = conversationThreadsState,
        realtimeConnectionState = realtimeConnectionState,
        authFormState = authStateModel.authFormState,
        registerFormState = authStateModel.registerFormState,
        callSessionState = callSessionState,
        contactsState = contactsState,
        walletStateModel = walletStateModel,
        friends = friends,
        selectedConversationId = selectedConversationId,
        chatStatusMessage = chatStatusMessage,
        chatStatusError = chatStatusError,
        friendRequestsSnapshot = friendRequestsSnapshot,
        friendRequestsStatusMessage = friendRequestsStatusMessage,
        friendRequestsStatusError = friendRequestsStatusError,
    )
    val latestConversationThreadsState by rememberUpdatedState(conversationThreadsState)
    val latestSelectedConversationId by rememberUpdatedState(selectedConversationId)
    val latestAppSessionState by rememberUpdatedState(appSessionState)
    val latestCurrentRoute by rememberUpdatedState(currentRoute)
    val latestRealtimeConnectionState by rememberUpdatedState(realtimeConnectionState)
    val latestRealtimeEnabled by rememberUpdatedState(
        appSessionState.isAuthenticated && sessionCredentialsStore.current.hasAccessToken
    )
    val sessionBoundaryRuntime = AppSessionBoundaryRuntime(
        appShellCoordinator = appShellCoordinator,
        refreshSessionOnce = { session, setSession ->
            sessionRefreshRuntime.refreshOnce(
                session = session,
                setSession = setSession,
            )
        },
        startSessionRefreshLoop = { onUnauthorized ->
            sessionRefreshRuntime.startLoopIfNeeded(
                getSession = { latestAppSessionState },
                getConnectionState = { latestRealtimeConnectionState },
                setSession = onSessionRefreshed,
                onUnauthorized = onUnauthorized,
            )
        },
        cancelSessionRefreshLoop = sessionRefreshRuntime::cancel,
        invalidateWalletRequests = walletStateHolder::invalidate,
        cancelCallSession = callSessionRuntime::cancel,
    )
    val sessionBoundaryCallbacks = AppSessionBoundaryCallbacks(
        onHydratedConversationAccountChanged = onHydratedConversationAccountChanged,
        onCallSessionStateChanged = onCallSessionStateChanged,
        onWalletStateChanged = walletStateHolder::replaceState,
        onConversationThreadsChanged = onConversationThreadsChanged,
        onAuthenticatedSessionApplied = onAuthenticatedSessionApplied,
        onAuthFormStateChanged = authStateHolder::replaceAuthFormState,
        onSessionCleared = onSessionCleared,
        onRegisterFormStateChanged = authStateHolder::replaceRegisterFormState,
        onContactsStateChanged = onContactsStateChanged,
        onFriendsChanged = onFriendsChanged,
        onFriendRequestStatusReset = onFriendRequestStatusReset,
    )
    val routeActionBindings = AppRouteActionBindings(
        state = state,
        authStateHolder = authStateHolder,
        walletStateHolder = walletStateHolder,
        callbacks = callbacks,
        routeUiState = routeUiState,
        routeRuntimes = routeRuntimes,
        previewThreadsState = previewThreadsState,
        sessionBoundaryRuntime = sessionBoundaryRuntime,
        sessionBoundaryCallbacks = sessionBoundaryCallbacks,
        getLatestCurrentRoute = { latestCurrentRoute },
    )
    AppRouteHostEffects(
        navController = navController,
        dependencies = dependencies,
        state = state,
        callbacks = callbacks,
        routeRuntimes = routeRuntimes,
        previewThreadsState = previewThreadsState,
        sessionBoundaryRuntime = sessionBoundaryRuntime,
        sessionBoundaryCallbacks = sessionBoundaryCallbacks,
        routeActionBindings = routeActionBindings,
        getLatestConversationThreadsState = { latestConversationThreadsState },
        getLatestSelectedConversationId = { latestSelectedConversationId },
        getLatestAppSessionState = { latestAppSessionState },
        getLatestCurrentRoute = { latestCurrentRoute },
        isRealtimeEnabled = { latestRealtimeEnabled },
    )

    ArgusNavHost(
        navController = navController,
        startDestination = startDestination,
        currentRoute = currentRoute,
        routeUiState = routeUiState,
        walletStateModel = walletStateModel,
        appRouteNavigationRuntime = appRouteNavigationRuntime,
        onShellDestinationSelected = routeActionBindings::openShellDestination,
        onAuthAction = routeActionBindings::handleAuthAction,
        onRegisterAction = routeActionBindings::handleRegisterAction,
        onInboxAction = routeActionBindings::handleInboxAction,
        onContactsAction = routeActionBindings::handleContactsAction,
        onNewFriendsAction = routeActionBindings::handleNewFriendsAction,
        onCallSessionAction = routeActionBindings::handleCallSessionAction,
        onWalletAction = routeActionBindings::handleWalletAction,
        onSignOut = { routeActionBindings.signOutToEntry() },
        onChatAction = routeActionBindings::handleChatAction,
    )
}

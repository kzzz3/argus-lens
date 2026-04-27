package com.kzzz3.argus.lens.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kzzz3.argus.lens.app.navigation.AppRoute
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun AppRouteHost(
    dependencies: AppDependencies,
    runtimeScope: CoroutineScope,
    state: AppRouteHostState,
    callbacks: AppRouteHostCallbacks,
) {
    val appSessionState = state.appSessionState
    val conversationThreadsState = state.conversationThreadsState
    val currentRoute = state.currentRoute
    val authFormState = state.authFormState
    val registerFormState = state.registerFormState
    val callSessionState = state.callSessionState
    val contactsState = state.contactsState
    val walletStateModel = state.walletStateModel
    val friends = state.friends
    val selectedConversationId = state.selectedConversationId
    val chatStatusMessage = state.chatStatusMessage
    val chatStatusError = state.chatStatusError
    val friendRequestsSnapshot = state.friendRequestsSnapshot
    val friendRequestsStatusMessage = state.friendRequestsStatusMessage
    val friendRequestsStatusError = state.friendRequestsStatusError
    val hydratedConversationAccountId = state.hydratedConversationAccountId
    val realtimeConnectionState = state.realtimeConnectionState
    val realtimeLastEventId = state.realtimeLastEventId
    val realtimeReconnectGeneration = state.realtimeReconnectGeneration
    val onRouteChanged = callbacks.onRouteChanged
    val onAuthFormStateChanged = callbacks.onAuthFormStateChanged
    val onRegisterFormStateChanged = callbacks.onRegisterFormStateChanged
    val onCallSessionStateChanged = callbacks.onCallSessionStateChanged
    val onContactsStateChanged = callbacks.onContactsStateChanged
    val onWalletStateChanged = callbacks.onWalletStateChanged
    val onFriendsChanged = callbacks.onFriendsChanged
    val onChatStatusCleared = callbacks.onChatStatusCleared
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
    val realtimeClient = dependencies.realtimeClient
    val callSessionRuntime = routeRuntimes.callSessionRuntime
    val sessionRefreshRuntime = routeRuntimes.sessionRefreshRuntime
    val walletRequestRuntime = routeRuntimes.walletRequestRuntime
    val realtimeConnectionRuntime = routeRuntimes.realtimeConnectionRuntime
    val appPersistenceRuntime = routeRuntimes.appPersistenceRuntime
    val appInitialHydrationRuntime = routeRuntimes.appInitialHydrationRuntime
    val appRouteLoadRuntime = routeRuntimes.appRouteLoadRuntime
    val appRouteNavigationRuntime = routeRuntimes.appRouteNavigationRuntime
    val previewThreadsState = remember {
        appShellCoordinator.createPreviewConversationThreads(
            currentUserDisplayName = DEFAULT_PREVIEW_DISPLAY_NAME,
        )
    }
    val initialSessionSnapshot = dependencies.initialSessionSnapshot
    val startDestination = remember { currentRoute.name }
    val routeUiState = rememberAppRouteUiState(
        appSessionState = appSessionState,
        conversationThreadsState = conversationThreadsState,
        realtimeConnectionState = realtimeConnectionState,
        authFormState = authFormState,
        registerFormState = registerFormState,
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
        invalidateWalletRequests = walletRequestRuntime::invalidate,
        cancelCallSession = callSessionRuntime::cancel,
    )
    val sessionBoundaryCallbacks = AppSessionBoundaryCallbacks(
        onHydratedConversationAccountChanged = onHydratedConversationAccountChanged,
        onCallSessionStateChanged = onCallSessionStateChanged,
        onWalletStateChanged = onWalletStateChanged,
        onConversationThreadsChanged = onConversationThreadsChanged,
        onAuthenticatedSessionApplied = onAuthenticatedSessionApplied,
        onAuthFormStateChanged = onAuthFormStateChanged,
        onSessionCleared = onSessionCleared,
        onRegisterFormStateChanged = onRegisterFormStateChanged,
        onContactsStateChanged = onContactsStateChanged,
        onFriendsChanged = onFriendsChanged,
        onFriendRequestStatusReset = onFriendRequestStatusReset,
    )
    val routeActionBindings = AppRouteActionBindings(
        state = state,
        callbacks = callbacks,
        routeUiState = routeUiState,
        routeRuntimes = routeRuntimes,
        previewThreadsState = previewThreadsState,
        sessionBoundaryRuntime = sessionBoundaryRuntime,
        sessionBoundaryCallbacks = sessionBoundaryCallbacks,
        getLatestCurrentRoute = { latestCurrentRoute },
    )
    LaunchedEffect(initialSessionSnapshot.isAuthenticated, initialSessionSnapshot.accountId) {
        appInitialHydrationRuntime.hydrate(
            request = AppInitialHydrationRequest(
                initialSession = initialSessionSnapshot,
                initialCredentials = dependencies.initialSessionCredentials,
                previewThreadsState = previewThreadsState,
            ),
            callbacks = AppInitialHydrationCallbacks(
                onConversationThreadsChanged = onConversationThreadsChanged,
                onHydratedConversationAccountChanged = onHydratedConversationAccountChanged,
                onRouteChanged = onRouteChanged,
                onHydratedSessionApplied = onHydratedSessionApplied,
            ),
        )
    }

    LaunchedEffect(appSessionState) {
        appPersistenceRuntime.persistSession(appSessionState, sessionCredentialsStore.current)
    }

    LaunchedEffect(selectedConversationId) {
        onChatStatusCleared()
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, appSessionState.displayName) {
        sessionBoundaryRuntime.applySessionBoundary(
            session = appSessionState,
            callbacks = sessionBoundaryCallbacks,
        )
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, realtimeReconnectGeneration) {
        realtimeConnectionRuntime.connect(
            request = RealtimeConnectionRequest(
                isAuthenticated = appSessionState.isAuthenticated,
                accountId = appSessionState.accountId,
                credentials = sessionCredentialsStore.current,
                lastEventId = realtimeLastEventId,
                reconnectGeneration = realtimeReconnectGeneration,
                isRealtimeEnabled = { latestRealtimeEnabled },
                getSession = { latestAppSessionState },
                getConversationThreadsState = { latestConversationThreadsState },
                getSelectedConversationId = { latestSelectedConversationId },
                getCurrentRoute = { latestCurrentRoute },
            ),
            callbacks = routeActionBindings.realtimeConnectionCallbacks(),
        )
    }

    DisposableEffect(realtimeClient) {
        onDispose {
            realtimeConnectionRuntime.dispose(routeActionBindings.realtimeConnectionCallbacks())
        }
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, hydratedConversationAccountId, conversationThreadsState) {
        appPersistenceRuntime.persistConversationThreads(
            session = appSessionState,
            hydratedConversationAccountId = hydratedConversationAccountId,
            state = conversationThreadsState,
        )
    }

    LaunchedEffect(currentRoute, appSessionState.isAuthenticated) {
        appRouteLoadRuntime.loadForRoute(
            request = AppRouteLoadRequest(
                route = currentRoute,
                isAuthenticated = appSessionState.isAuthenticated,
                friendRequestsSnapshot = friendRequestsSnapshot,
            ),
            callbacks = AppRouteLoadCallbacks(
                onFriendsChanged = onFriendsChanged,
                onFriendRequestStatusChanged = routeActionBindings::applyFriendRequestStatus,
            ),
        )
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentRoute, currentBackStackEntry?.destination?.route) {
        if (currentBackStackEntry?.destination?.route != currentRoute.name) {
            navController.navigate(currentRoute.name) {
                launchSingleTop = true
            }
        }
    }

    AppRouteNavGraph(
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

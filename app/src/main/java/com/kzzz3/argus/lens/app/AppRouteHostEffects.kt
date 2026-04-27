package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.model.session.AppSessionState

@Composable
internal fun AppRouteHostEffects(
    navController: NavHostController,
    dependencies: AppDependencies,
    state: AppRouteHostState,
    callbacks: AppRouteHostCallbacks,
    routeRuntimes: AppRouteRuntimes,
    previewThreadsState: ConversationThreadsState,
    sessionBoundaryRuntime: AppSessionBoundaryRuntime,
    sessionBoundaryCallbacks: AppSessionBoundaryCallbacks,
    routeActionBindings: AppRouteActionBindings,
    getLatestConversationThreadsState: () -> ConversationThreadsState,
    getLatestSelectedConversationId: () -> String,
    getLatestAppSessionState: () -> AppSessionState,
    getLatestCurrentRoute: () -> AppRoute,
    isRealtimeEnabled: () -> Boolean,
) {
    val appSessionState = state.appSessionState
    val conversationThreadsState = state.conversationThreadsState
    val currentRoute = state.currentRoute
    val selectedConversationId = state.selectedConversationId
    val friendRequestsSnapshot = state.friendRequestsSnapshot
    val hydratedConversationAccountId = state.hydratedConversationAccountId
    val realtimeLastEventId = state.realtimeLastEventId
    val realtimeReconnectGeneration = state.realtimeReconnectGeneration
    val initialSessionSnapshot = dependencies.initialSessionSnapshot
    val sessionCredentialsStore = dependencies.sessionCredentialsStore
    val realtimeClient = dependencies.realtimeClient

    LaunchedEffect(initialSessionSnapshot.isAuthenticated, initialSessionSnapshot.accountId) {
        routeRuntimes.appInitialHydrationRuntime.hydrate(
            request = AppInitialHydrationRequest(
                initialSession = initialSessionSnapshot,
                initialCredentials = dependencies.initialSessionCredentials,
                previewThreadsState = previewThreadsState,
            ),
            callbacks = AppInitialHydrationCallbacks(
                onConversationThreadsChanged = callbacks.onConversationThreadsChanged,
                onHydratedConversationAccountChanged = callbacks.onHydratedConversationAccountChanged,
                onRouteChanged = callbacks.onRouteChanged,
                onHydratedSessionApplied = callbacks.onHydratedSessionApplied,
            ),
        )
    }

    LaunchedEffect(appSessionState) {
        routeRuntimes.appPersistenceRuntime.persistSession(appSessionState, sessionCredentialsStore.current)
    }

    LaunchedEffect(selectedConversationId) {
        callbacks.onChatStatusCleared()
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, appSessionState.displayName) {
        sessionBoundaryRuntime.applySessionBoundary(
            session = appSessionState,
            callbacks = sessionBoundaryCallbacks,
        )
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, realtimeReconnectGeneration) {
        routeRuntimes.realtimeConnectionRuntime.connect(
            request = RealtimeConnectionRequest(
                isAuthenticated = appSessionState.isAuthenticated,
                accountId = appSessionState.accountId,
                credentials = sessionCredentialsStore.current,
                lastEventId = realtimeLastEventId,
                reconnectGeneration = realtimeReconnectGeneration,
                isRealtimeEnabled = isRealtimeEnabled,
                getSession = getLatestAppSessionState,
                getConversationThreadsState = getLatestConversationThreadsState,
                getSelectedConversationId = getLatestSelectedConversationId,
                getCurrentRoute = getLatestCurrentRoute,
            ),
            callbacks = routeActionBindings.realtimeConnectionCallbacks(),
        )
    }

    DisposableEffect(realtimeClient) {
        onDispose {
            routeRuntimes.realtimeConnectionRuntime.dispose(routeActionBindings.realtimeConnectionCallbacks())
        }
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, hydratedConversationAccountId, conversationThreadsState) {
        routeRuntimes.appPersistenceRuntime.persistConversationThreads(
            session = appSessionState,
            hydratedConversationAccountId = hydratedConversationAccountId,
            state = conversationThreadsState,
        )
    }

    LaunchedEffect(currentRoute, appSessionState.isAuthenticated) {
        routeRuntimes.appRouteLoadRuntime.loadForRoute(
            request = AppRouteLoadRequest(
                route = currentRoute,
                isAuthenticated = appSessionState.isAuthenticated,
                friendRequestsSnapshot = friendRequestsSnapshot,
            ),
            callbacks = AppRouteLoadCallbacks(
                onFriendsChanged = callbacks.onFriendsChanged,
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
}

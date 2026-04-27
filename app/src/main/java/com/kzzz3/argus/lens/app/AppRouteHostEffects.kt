package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeClient
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.feature.auth.navigation.AuthGraphRoute
import com.kzzz3.argus.lens.feature.inbox.ChatStateHolder
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxStateHolder
import com.kzzz3.argus.lens.feature.realtime.buildRealtimeStatusLabel
import com.kzzz3.argus.lens.model.session.AppSessionState
import com.kzzz3.argus.lens.navigation.MainGraphRoute
import com.kzzz3.argus.lens.navigation.graphRouteForAppRoute

internal data class AppRouteHostEffectDependencies(
    val initialSessionSnapshot: AppSessionState,
    val initialSessionCredentials: SessionCredentials,
    val sessionCredentialsStore: SessionCredentialsStore,
    val realtimeClient: ConversationRealtimeClient,
)

@Composable
internal fun AppRouteHostEffects(
    navController: NavHostController,
    effectDependencies: AppRouteHostEffectDependencies,
    state: AppRouteHostState,
    callbacks: AppRouteHostCallbacks,
    routeRuntimes: AppRouteRuntimes,
    chatStateHolder: ChatStateHolder,
    inboxStateHolder: InboxStateHolder,
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
    val chatStatusMessage = state.chatStatusMessage
    val chatStatusError = state.chatStatusError
    val realtimeConnectionState = state.realtimeConnectionState
    val friendRequestsSnapshot = state.friendRequestsSnapshot
    val hydratedConversationAccountId = state.hydratedConversationAccountId
    val realtimeLastEventId = state.realtimeLastEventId
    val realtimeReconnectGeneration = state.realtimeReconnectGeneration
    val initialSessionSnapshot = effectDependencies.initialSessionSnapshot
    val sessionCredentialsStore = effectDependencies.sessionCredentialsStore
    val realtimeClient = effectDependencies.realtimeClient

    LaunchedEffect(initialSessionSnapshot.isAuthenticated, initialSessionSnapshot.accountId) {
        routeRuntimes.appInitialHydrationRuntime.hydrate(
            request = AppInitialHydrationRequest(
                initialSession = initialSessionSnapshot,
                initialCredentials = effectDependencies.initialSessionCredentials,
                previewThreadsState = previewThreadsState,
                restorableEntryContext = state.restorableEntryContext,
            ),
            callbacks = AppInitialHydrationCallbacks(
                onConversationThreadsChanged = callbacks.onConversationThreadsChanged,
                onHydratedConversationAccountChanged = callbacks.onHydratedConversationAccountChanged,
                onRouteChanged = callbacks.onRouteChanged,
                onHydratedSessionApplied = callbacks.onHydratedSessionApplied,
                onSelectedConversationChanged = callbacks.onSelectedConversationChanged,
                onRestorableEntryContextCleared = callbacks.onRestorableEntryContextCleared,
            ),
        )
    }

    LaunchedEffect(appSessionState) {
        routeRuntimes.appPersistenceRuntime.persistSession(appSessionState, sessionCredentialsStore.current)
    }

    LaunchedEffect(selectedConversationId) {
        callbacks.onChatStatusCleared()
    }

    LaunchedEffect(
        appSessionState.displayName,
        conversationThreadsState,
        selectedConversationId,
        chatStatusMessage,
        chatStatusError,
    ) {
        chatStateHolder.replaceInputs(
            currentUserDisplayName = resolvePreviewDisplayName(appSessionState.displayName),
            threadsState = conversationThreadsState,
            selectedConversationId = selectedConversationId,
            statusMessage = chatStatusMessage,
            isStatusError = chatStatusError,
        )
    }

    LaunchedEffect(appSessionState, conversationThreadsState, realtimeConnectionState) {
        inboxStateHolder.replaceInputs(
            sessionState = appSessionState,
            threadsState = conversationThreadsState,
            realtimeStatusLabel = buildRealtimeStatusLabel(realtimeConnectionState),
            shellStatusLabel = resolveShellStatusLabel(appSessionState, realtimeConnectionState),
        )
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
    val currentDestinationRoute = currentBackStackEntry?.destination?.route
    val previousGraphRoute = currentBackStackEntry
        ?.destination
        ?.hierarchy
        ?.firstOrNull { destination -> destination.route == AuthGraphRoute || destination.route == MainGraphRoute }
        ?.route
    val targetGraphRoute = graphRouteForAppRoute(currentRoute)
    val targetNavigationRoute = routeRuntimes.appRouteNavigationRuntime.buildNavigationRoute(currentRoute)
    LaunchedEffect(currentRoute, targetNavigationRoute, currentDestinationRoute, previousGraphRoute) {
        if (currentDestinationRoute != targetNavigationRoute) {
            navController.navigate(targetNavigationRoute) {
                launchSingleTop = true
                if (previousGraphRoute != null && previousGraphRoute != targetGraphRoute) {
                    popUpTo(previousGraphRoute) {
                        inclusive = true
                    }
                }
            }
        }
    }
}

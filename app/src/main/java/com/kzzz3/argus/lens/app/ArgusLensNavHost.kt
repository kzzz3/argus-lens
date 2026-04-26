package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState

private object ArgusLensGraph {
    const val App = "app"
}

@Composable
fun ArgusLensNavHost(
    dependencies: AppDependencies,
    currentRoute: AppRoute,
    selectedConversationId: String,
    hydratedConversationAccountId: String?,
    realtimeConnectionState: ConversationRealtimeConnectionState,
    realtimeLastEventId: String,
    realtimeReconnectGeneration: Int,
    onRouteChanged: (AppRoute) -> Unit,
    onConversationOpened: (String) -> Unit,
    onConversationSelectionCleared: () -> Unit,
    onHydratedConversationAccountChanged: (String?) -> Unit,
    onRealtimeConnectionStateChanged: (ConversationRealtimeConnectionState) -> Unit,
    onRealtimeEventIdRecorded: (String) -> Unit,
    onRealtimeLastEventIdReset: () -> Unit,
    onRealtimeReconnectIncremented: () -> Unit,
    onRealtimeReconnectIncrementedBy: (Int) -> Unit,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = ArgusLensGraph.App,
    ) {
        composable(ArgusLensGraph.App) {
            AppRouteHost(
                dependencies = dependencies,
                currentRoute = currentRoute,
                selectedConversationId = selectedConversationId,
                hydratedConversationAccountId = hydratedConversationAccountId,
                realtimeConnectionState = realtimeConnectionState,
                realtimeLastEventId = realtimeLastEventId,
                realtimeReconnectGeneration = realtimeReconnectGeneration,
                onRouteChanged = onRouteChanged,
                onConversationOpened = onConversationOpened,
                onConversationSelectionCleared = onConversationSelectionCleared,
                onHydratedConversationAccountChanged = onHydratedConversationAccountChanged,
                onRealtimeConnectionStateChanged = onRealtimeConnectionStateChanged,
                onRealtimeEventIdRecorded = onRealtimeEventIdRecorded,
                onRealtimeLastEventIdReset = onRealtimeLastEventIdReset,
                onRealtimeReconnectIncremented = onRealtimeReconnectIncremented,
                onRealtimeReconnectIncrementedBy = onRealtimeReconnectIncrementedBy,
            )
        }
    }
}

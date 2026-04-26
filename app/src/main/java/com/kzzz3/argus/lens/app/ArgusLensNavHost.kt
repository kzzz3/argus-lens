package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.model.session.AppSessionState

private object ArgusLensGraph {
    const val App = "app"
}

@Composable
fun ArgusLensNavHost(
    dependencies: AppDependencies,
    appSessionState: AppSessionState,
    currentRoute: AppRoute,
    authFormState: AuthFormState,
    registerFormState: RegisterFormState,
    selectedConversationId: String,
    hydratedConversationAccountId: String?,
    realtimeConnectionState: ConversationRealtimeConnectionState,
    realtimeLastEventId: String,
    realtimeReconnectGeneration: Int,
    onRouteChanged: (AppRoute) -> Unit,
    onAuthFormStateChanged: (AuthFormState) -> Unit,
    onRegisterFormStateChanged: (RegisterFormState) -> Unit,
    onConversationOpened: (String) -> Unit,
    onConversationSelectionCleared: () -> Unit,
    onHydratedSessionApplied: (AppSessionState, String?) -> Unit,
    onAuthenticatedSessionApplied: (AppSessionState, SessionCredentials, String, Int) -> Unit,
    onSessionRefreshed: (AppSessionState) -> Unit,
    onSessionCleared: () -> Unit,
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
                appSessionState = appSessionState,
                currentRoute = currentRoute,
                authFormState = authFormState,
                registerFormState = registerFormState,
                selectedConversationId = selectedConversationId,
                hydratedConversationAccountId = hydratedConversationAccountId,
                realtimeConnectionState = realtimeConnectionState,
                realtimeLastEventId = realtimeLastEventId,
                realtimeReconnectGeneration = realtimeReconnectGeneration,
                onRouteChanged = onRouteChanged,
                onAuthFormStateChanged = onAuthFormStateChanged,
                onRegisterFormStateChanged = onRegisterFormStateChanged,
                onConversationOpened = onConversationOpened,
                onConversationSelectionCleared = onConversationSelectionCleared,
                onHydratedSessionApplied = onHydratedSessionApplied,
                onAuthenticatedSessionApplied = onAuthenticatedSessionApplied,
                onSessionRefreshed = onSessionRefreshed,
                onSessionCleared = onSessionCleared,
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

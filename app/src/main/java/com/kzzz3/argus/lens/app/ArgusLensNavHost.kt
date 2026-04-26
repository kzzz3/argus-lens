package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kzzz3.argus.lens.app.navigation.AppRoute

private object ArgusLensGraph {
    const val App = "app"
}

@Composable
fun ArgusLensNavHost(
    dependencies: AppDependencies,
    currentRoute: AppRoute,
    selectedConversationId: String,
    onRouteChanged: (AppRoute) -> Unit,
    onConversationOpened: (String) -> Unit,
    onConversationSelectionCleared: () -> Unit,
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
                onRouteChanged = onRouteChanged,
                onConversationOpened = onConversationOpened,
                onConversationSelectionCleared = onConversationSelectionCleared,
            )
        }
    }
}

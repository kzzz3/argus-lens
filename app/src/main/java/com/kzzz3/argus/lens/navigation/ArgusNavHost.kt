package com.kzzz3.argus.lens.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.navigation.graphRoute
import com.kzzz3.argus.lens.feature.auth.navigation.AuthRoutes
import com.kzzz3.argus.lens.feature.auth.navigation.authGraph

internal data class ArgusNavRoutes(
    val auth: AuthRoutes,
    val main: MainRoutes,
)

@Composable
internal fun ArgusNavHost(
    navController: NavHostController,
    startDestination: Any,
    routes: ArgusNavRoutes,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        authGraph(routes.auth)
        mainGraph(routes.main)
    }
}

internal fun graphRouteForAppRoute(route: AppRoute): String {
    return route.graphRoute
}

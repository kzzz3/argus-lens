package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private object ArgusLensGraph {
    const val App = "app"
}

@Composable
fun ArgusLensNavHost(
    dependencies: AppDependencies,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = ArgusLensGraph.App,
    ) {
        composable(ArgusLensGraph.App) {
            AppRouteHost(dependencies = dependencies)
        }
    }
}

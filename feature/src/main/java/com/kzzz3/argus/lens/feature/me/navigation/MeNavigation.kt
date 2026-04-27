package com.kzzz3.argus.lens.feature.me.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kzzz3.argus.lens.feature.me.MeScreen
import com.kzzz3.argus.lens.feature.me.MeUiState
import com.kzzz3.argus.lens.feature.navigation.AuthenticatedFeatureRouteShell
import com.kzzz3.argus.lens.ui.shell.ShellDestination

const val MeRoute = "Me"

fun NavGraphBuilder.meNavigation(
    meShellDestination: ShellDestination,
    onTabSelected: (ShellDestination) -> Unit,
    meState: MeUiState,
    onSignOut: () -> Unit,
) {
    composable(MeRoute) {
        AuthenticatedFeatureRouteShell(
            currentDestination = meShellDestination,
            onTabSelected = onTabSelected,
        ) { contentModifier ->
            MeScreen(
                state = meState,
                onSignOut = onSignOut,
                modifier = contentModifier,
            )
        }
    }
}

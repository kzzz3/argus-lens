package com.kzzz3.argus.lens.feature.me.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kzzz3.argus.lens.feature.me.MeScreen
import com.kzzz3.argus.lens.feature.me.MeUiState
import com.kzzz3.argus.lens.feature.navigation.AuthenticatedFeatureRouteShell
import com.kzzz3.argus.lens.ui.shell.ShellDestination
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val MeRoutePattern = "Me"

@Serializable
@SerialName(MeRoutePattern)
data object MeRoute

data class MeRoutes(
    val meShellDestination: ShellDestination,
    val onTabSelected: (ShellDestination) -> Unit,
    val meState: MeUiState,
    val onSignOut: () -> Unit,
)

fun NavGraphBuilder.meNavigation(
    routes: MeRoutes,
) {
    composable<MeRoute> {
        AuthenticatedFeatureRouteShell(
            currentDestination = routes.meShellDestination,
            onTabSelected = routes.onTabSelected,
        ) { contentModifier ->
            MeScreen(
                state = routes.meState,
                onSignOut = routes.onSignOut,
                modifier = contentModifier,
            )
        }
    }
}

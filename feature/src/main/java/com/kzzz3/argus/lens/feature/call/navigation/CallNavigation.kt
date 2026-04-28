package com.kzzz3.argus.lens.feature.call.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kzzz3.argus.lens.feature.call.CallSessionAction
import com.kzzz3.argus.lens.feature.call.CallSessionScreen
import com.kzzz3.argus.lens.feature.call.CallSessionUiState
import com.kzzz3.argus.lens.feature.navigation.AuthenticatedFeatureRouteShell
import com.kzzz3.argus.lens.ui.shell.ShellDestination
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val CallSessionRoutePattern = "CallSession"

@Serializable
@SerialName(CallSessionRoutePattern)
data object CallSessionRoute

data class CallSessionRoutes(
    val callShellDestination: ShellDestination,
    val onTabSelected: (ShellDestination) -> Unit,
    val callSessionState: CallSessionUiState,
    val onCallSessionAction: (CallSessionAction) -> Unit,
)

fun NavGraphBuilder.callNavigation(
    routes: CallSessionRoutes,
) {
    composable<CallSessionRoute> {
        AuthenticatedFeatureRouteShell(
            currentDestination = routes.callShellDestination,
            onTabSelected = routes.onTabSelected,
        ) { contentModifier ->
            CallSessionScreen(
                state = routes.callSessionState,
                onAction = routes.onCallSessionAction,
                modifier = contentModifier,
            )
        }
    }
}

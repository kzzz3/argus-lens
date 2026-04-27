package com.kzzz3.argus.lens.feature.call.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kzzz3.argus.lens.feature.call.CallSessionAction
import com.kzzz3.argus.lens.feature.call.CallSessionScreen
import com.kzzz3.argus.lens.feature.call.CallSessionUiState
import com.kzzz3.argus.lens.feature.navigation.AuthenticatedFeatureRouteShell
import com.kzzz3.argus.lens.ui.shell.ShellDestination

const val CallSessionRoute = "CallSession"

fun NavGraphBuilder.callNavigation(
    callShellDestination: ShellDestination,
    onTabSelected: (ShellDestination) -> Unit,
    callSessionState: CallSessionUiState,
    onCallSessionAction: (CallSessionAction) -> Unit,
) {
    composable(CallSessionRoute) {
        AuthenticatedFeatureRouteShell(
            currentDestination = callShellDestination,
            onTabSelected = onTabSelected,
        ) { contentModifier ->
            CallSessionScreen(
                state = callSessionState,
                onAction = onCallSessionAction,
                modifier = contentModifier,
            )
        }
    }
}

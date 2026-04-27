package com.kzzz3.argus.lens.feature.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kzzz3.argus.lens.ui.shell.AuthenticatedShell
import com.kzzz3.argus.lens.ui.shell.ShellDestination

@Composable
fun AuthenticatedFeatureRouteShell(
    currentDestination: ShellDestination,
    onTabSelected: (ShellDestination) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    AuthenticatedShell(
        currentDestination = currentDestination,
        onTabSelected = onTabSelected,
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}

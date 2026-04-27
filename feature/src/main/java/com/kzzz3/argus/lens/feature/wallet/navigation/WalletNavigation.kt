package com.kzzz3.argus.lens.feature.wallet.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kzzz3.argus.lens.feature.navigation.AuthenticatedFeatureRouteShell
import com.kzzz3.argus.lens.feature.wallet.WalletAction
import com.kzzz3.argus.lens.feature.wallet.WalletScreen
import com.kzzz3.argus.lens.feature.wallet.WalletUiState
import com.kzzz3.argus.lens.ui.shell.ShellDestination

const val WalletRoute = "Wallet"

fun NavGraphBuilder.walletNavigation(
    walletShellDestination: ShellDestination,
    onTabSelected: (ShellDestination) -> Unit,
    walletState: WalletUiState,
    permissionRequestPending: Boolean,
    onWalletAction: (WalletAction) -> Unit,
) {
    composable(WalletRoute) {
        AuthenticatedFeatureRouteShell(
            currentDestination = walletShellDestination,
            onTabSelected = onTabSelected,
        ) { contentModifier ->
            WalletScreen(
                state = walletState,
                permissionRequestPending = permissionRequestPending,
                onAction = onWalletAction,
                modifier = contentModifier,
            )
        }
    }
}

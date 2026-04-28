package com.kzzz3.argus.lens.feature.wallet.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kzzz3.argus.lens.feature.navigation.AuthenticatedFeatureRouteShell
import com.kzzz3.argus.lens.feature.wallet.WalletAction
import com.kzzz3.argus.lens.feature.wallet.WalletScreen
import com.kzzz3.argus.lens.feature.wallet.WalletUiState
import com.kzzz3.argus.lens.ui.shell.ShellDestination
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val WalletRoutePattern = "Wallet"

@Serializable
@SerialName(WalletRoutePattern)
data object WalletRoute

data class WalletRoutes(
    val walletShellDestination: ShellDestination,
    val onTabSelected: (ShellDestination) -> Unit,
    val walletState: WalletUiState,
    val permissionRequestPending: Boolean,
    val onWalletAction: (WalletAction) -> Unit,
)

fun NavGraphBuilder.walletNavigation(
    routes: WalletRoutes,
) {
    composable<WalletRoute> {
        AuthenticatedFeatureRouteShell(
            currentDestination = routes.walletShellDestination,
            onTabSelected = routes.onTabSelected,
        ) { contentModifier ->
            WalletScreen(
                state = routes.walletState,
                permissionRequestPending = routes.permissionRequestPending,
                onAction = routes.onWalletAction,
                modifier = contentModifier,
            )
        }
    }
}

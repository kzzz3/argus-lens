package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.feature.wallet.withCurrentAccount
import com.kzzz3.argus.lens.ui.shell.ShellDestination

internal data class AppRouteNavigationRequest(
    val accountId: String,
    val walletState: WalletState,
)

internal data class AppRouteNavigationCallbacks(
    val onWalletStateChanged: (WalletState) -> Unit,
    val onRouteChanged: (AppRoute) -> Unit,
)

internal class AppRouteNavigationRuntime {
    fun openTopLevelRoute(
        route: AppRoute,
        request: AppRouteNavigationRequest,
        callbacks: AppRouteNavigationCallbacks,
    ) {
        if (route == AppRoute.Wallet) {
            callbacks.onWalletStateChanged(request.walletState.withCurrentAccount(request.accountId))
        }
        callbacks.onRouteChanged(route)
    }

    fun openShellDestination(
        destination: ShellDestination,
        request: AppRouteNavigationRequest,
        callbacks: AppRouteNavigationCallbacks,
    ) {
        when (destination) {
            ShellDestination.Inbox -> openTopLevelRoute(AppRoute.Inbox, request, callbacks)
            ShellDestination.Contacts -> openTopLevelRoute(AppRoute.Contacts, request, callbacks)
            ShellDestination.Wallet -> openTopLevelRoute(AppRoute.Wallet, request, callbacks)
            ShellDestination.Me -> openTopLevelRoute(AppRoute.Me, request, callbacks)
            ShellDestination.Secondary -> Unit
        }
    }

    fun toShellDestination(route: AppRoute): ShellDestination {
        return when (route) {
            AppRoute.Inbox -> ShellDestination.Inbox
            AppRoute.Contacts -> ShellDestination.Contacts
            AppRoute.Wallet -> ShellDestination.Wallet
            AppRoute.Me -> ShellDestination.Me
            AppRoute.AuthEntry,
            AppRoute.RegisterEntry,
            AppRoute.NewFriends,
            AppRoute.CallSession,
            AppRoute.Chat,
            -> ShellDestination.Secondary
        }
    }
}

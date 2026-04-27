package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.navigation.TopLevelDestination
import com.kzzz3.argus.lens.ui.shell.ShellDestination

internal data class AppRouteNavigationRequest(
    val accountId: String,
)

internal data class AppRouteNavigationCallbacks(
    val onWalletOpened: (String) -> Unit,
    val onRouteChanged: (AppRoute) -> Unit,
)

internal class AppRouteNavigationRuntime {
    fun openTopLevelRoute(
        route: AppRoute,
        request: AppRouteNavigationRequest,
        callbacks: AppRouteNavigationCallbacks,
    ) {
        if (route == AppRoute.Wallet) {
            callbacks.onWalletOpened(request.accountId)
        }
        callbacks.onRouteChanged(route)
    }

    fun openShellDestination(
        destination: ShellDestination,
        request: AppRouteNavigationRequest,
        callbacks: AppRouteNavigationCallbacks,
    ) {
        TopLevelDestination.fromShellDestination(destination)?.let { topLevelDestination ->
            openTopLevelRoute(topLevelDestination.route, request, callbacks)
        }
    }

    fun toShellDestination(route: AppRoute): ShellDestination {
        return TopLevelDestination.fromRoute(route)?.shellDestination ?: ShellDestination.Secondary
    }

    fun resolveRouteShellDestination(
        route: AppRoute,
        hasSelectedConversation: Boolean = true,
    ): ShellDestination {
        return when {
            route == AppRoute.NewFriends -> ShellDestination.Contacts
            route == AppRoute.Chat && !hasSelectedConversation -> ShellDestination.Inbox
            else -> toShellDestination(route)
        }
    }
}

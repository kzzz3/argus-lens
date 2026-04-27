package com.kzzz3.argus.lens.navigation

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.ui.shell.ShellDestination

enum class TopLevelDestination(
    val route: AppRoute,
    val shellDestination: ShellDestination,
) {
    Inbox(AppRoute.Inbox, ShellDestination.Inbox),
    Contacts(AppRoute.Contacts, ShellDestination.Contacts),
    Wallet(AppRoute.Wallet, ShellDestination.Wallet),
    Me(AppRoute.Me, ShellDestination.Me),
    ;

    companion object {
        fun fromRoute(route: AppRoute): TopLevelDestination? {
            return entries.firstOrNull { destination -> destination.route == route }
        }

        fun fromShellDestination(shellDestination: ShellDestination): TopLevelDestination? {
            return entries.firstOrNull { destination -> destination.shellDestination == shellDestination }
        }
    }
}

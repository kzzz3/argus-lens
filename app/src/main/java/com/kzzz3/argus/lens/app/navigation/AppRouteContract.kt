package com.kzzz3.argus.lens.app.navigation

import com.kzzz3.argus.lens.feature.auth.navigation.AuthGraphRoute
import com.kzzz3.argus.lens.navigation.MainGraphRoute
import com.kzzz3.argus.lens.ui.shell.ShellDestination

internal data class AppRouteDescriptor(
    val route: AppRoute,
    val routeString: String,
    val graphRoute: String,
    val shellDestination: ShellDestination,
    val requiresSelectedConversation: Boolean = false,
)

internal val AppRoute.routeDescriptor: AppRouteDescriptor
    get() = when (this) {
        AppRoute.AuthEntry -> AppRouteDescriptor(
            route = this,
            routeString = "AuthEntry",
            graphRoute = AuthGraphRoute,
            shellDestination = ShellDestination.Secondary,
        )
        AppRoute.RegisterEntry -> AppRouteDescriptor(
            route = this,
            routeString = "RegisterEntry",
            graphRoute = AuthGraphRoute,
            shellDestination = ShellDestination.Secondary,
        )
        AppRoute.Inbox -> AppRouteDescriptor(
            route = this,
            routeString = "Inbox",
            graphRoute = MainGraphRoute,
            shellDestination = ShellDestination.Inbox,
        )
        AppRoute.Contacts -> AppRouteDescriptor(
            route = this,
            routeString = "Contacts",
            graphRoute = MainGraphRoute,
            shellDestination = ShellDestination.Contacts,
        )
        AppRoute.NewFriends -> AppRouteDescriptor(
            route = this,
            routeString = "NewFriends",
            graphRoute = MainGraphRoute,
            shellDestination = ShellDestination.Secondary,
        )
        AppRoute.Wallet -> AppRouteDescriptor(
            route = this,
            routeString = "Wallet",
            graphRoute = MainGraphRoute,
            shellDestination = ShellDestination.Wallet,
        )
        AppRoute.Me -> AppRouteDescriptor(
            route = this,
            routeString = "Me",
            graphRoute = MainGraphRoute,
            shellDestination = ShellDestination.Me,
        )
        AppRoute.CallSession -> AppRouteDescriptor(
            route = this,
            routeString = "CallSession",
            graphRoute = MainGraphRoute,
            shellDestination = ShellDestination.Secondary,
        )
        AppRoute.Chat -> AppRouteDescriptor(
            route = this,
            routeString = "Chat",
            graphRoute = MainGraphRoute,
            shellDestination = ShellDestination.Secondary,
            requiresSelectedConversation = true,
        )
    }

internal val AppRoute.routeString: String
    get() = routeDescriptor.routeString

internal val AppRoute.graphRoute: String
    get() = routeDescriptor.graphRoute

package com.kzzz3.argus.lens.app.navigation

import com.kzzz3.argus.lens.feature.auth.navigation.AuthGraphRoutePattern
import com.kzzz3.argus.lens.feature.auth.navigation.LoginRoutePattern
import com.kzzz3.argus.lens.feature.auth.navigation.RegisterRoutePattern
import com.kzzz3.argus.lens.feature.call.navigation.CallSessionRoutePattern
import com.kzzz3.argus.lens.feature.contacts.navigation.ContactsRoutePattern
import com.kzzz3.argus.lens.feature.contacts.navigation.NewFriendsRoutePattern
import com.kzzz3.argus.lens.feature.inbox.navigation.ChatRoutePattern
import com.kzzz3.argus.lens.feature.inbox.navigation.InboxRoutePattern
import com.kzzz3.argus.lens.feature.me.navigation.MeRoutePattern
import com.kzzz3.argus.lens.feature.wallet.navigation.WalletRoutePattern
import com.kzzz3.argus.lens.navigation.MainGraphRoutePattern
import com.kzzz3.argus.lens.ui.shell.ShellDestination

internal data class AppRouteDescriptor(
    val route: AppRoute,
    val routeString: String,
    val graphRoute: String,
    val shellDestination: ShellDestination,
    val requiresActiveChatConversation: Boolean = false,
)

internal val AppRoute.routeDescriptor: AppRouteDescriptor
    get() = when (this) {
        AppRoute.AuthEntry -> AppRouteDescriptor(
            route = this,
            routeString = LoginRoutePattern,
            graphRoute = AuthGraphRoutePattern,
            shellDestination = ShellDestination.Secondary,
        )
        AppRoute.RegisterEntry -> AppRouteDescriptor(
            route = this,
            routeString = RegisterRoutePattern,
            graphRoute = AuthGraphRoutePattern,
            shellDestination = ShellDestination.Secondary,
        )
        AppRoute.Inbox -> AppRouteDescriptor(
            route = this,
            routeString = InboxRoutePattern,
            graphRoute = MainGraphRoutePattern,
            shellDestination = ShellDestination.Inbox,
        )
        AppRoute.Contacts -> AppRouteDescriptor(
            route = this,
            routeString = ContactsRoutePattern,
            graphRoute = MainGraphRoutePattern,
            shellDestination = ShellDestination.Contacts,
        )
        AppRoute.NewFriends -> AppRouteDescriptor(
            route = this,
            routeString = NewFriendsRoutePattern,
            graphRoute = MainGraphRoutePattern,
            shellDestination = ShellDestination.Secondary,
        )
        AppRoute.Wallet -> AppRouteDescriptor(
            route = this,
            routeString = WalletRoutePattern,
            graphRoute = MainGraphRoutePattern,
            shellDestination = ShellDestination.Wallet,
        )
        AppRoute.Me -> AppRouteDescriptor(
            route = this,
            routeString = MeRoutePattern,
            graphRoute = MainGraphRoutePattern,
            shellDestination = ShellDestination.Me,
        )
        AppRoute.CallSession -> AppRouteDescriptor(
            route = this,
            routeString = CallSessionRoutePattern,
            graphRoute = MainGraphRoutePattern,
            shellDestination = ShellDestination.Secondary,
        )
        AppRoute.Chat -> AppRouteDescriptor(
            route = this,
            routeString = ChatRoutePattern,
            graphRoute = MainGraphRoutePattern,
            shellDestination = ShellDestination.Secondary,
            requiresActiveChatConversation = true,
        )
    }

internal val AppRoute.routeString: String
    get() = routeDescriptor.routeString

internal val AppRoute.graphRoute: String
    get() = routeDescriptor.graphRoute

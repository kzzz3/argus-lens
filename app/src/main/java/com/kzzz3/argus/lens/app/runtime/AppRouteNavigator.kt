package com.kzzz3.argus.lens.app.runtime

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.navigation.routeDescriptor
import com.kzzz3.argus.lens.feature.auth.navigation.LoginRoute
import com.kzzz3.argus.lens.feature.auth.navigation.LoginRoutePattern
import com.kzzz3.argus.lens.feature.auth.navigation.RegisterRoute
import com.kzzz3.argus.lens.feature.auth.navigation.RegisterRoutePattern
import com.kzzz3.argus.lens.feature.auth.navigation.AuthGraphRoutePattern
import com.kzzz3.argus.lens.feature.call.navigation.CallSessionRoute
import com.kzzz3.argus.lens.feature.call.navigation.CallSessionRoutePattern
import com.kzzz3.argus.lens.feature.contacts.navigation.ContactsRoute
import com.kzzz3.argus.lens.feature.contacts.navigation.ContactsRoutePattern
import com.kzzz3.argus.lens.feature.contacts.navigation.NewFriendsRoute
import com.kzzz3.argus.lens.feature.contacts.navigation.NewFriendsRoutePattern
import com.kzzz3.argus.lens.feature.inbox.navigation.ChatThreadRoute
import com.kzzz3.argus.lens.feature.inbox.navigation.ChatRoutePattern
import com.kzzz3.argus.lens.feature.inbox.navigation.ChatThreadRoutePattern
import com.kzzz3.argus.lens.feature.inbox.navigation.InboxRoute
import com.kzzz3.argus.lens.feature.inbox.navigation.InboxRoutePattern
import com.kzzz3.argus.lens.feature.me.navigation.MeRoute
import com.kzzz3.argus.lens.feature.me.navigation.MeRoutePattern
import com.kzzz3.argus.lens.feature.wallet.navigation.WalletRoute
import com.kzzz3.argus.lens.feature.wallet.navigation.WalletRoutePattern
import com.kzzz3.argus.lens.feature.auth.navigation.AuthGraphRoute
import com.kzzz3.argus.lens.navigation.MainGraphRoute
import com.kzzz3.argus.lens.navigation.TopLevelDestination
import com.kzzz3.argus.lens.ui.shell.ShellDestination

internal data class AppRouteNavigationRequest(
    val accountId: String,
)

internal data class AppRouteNavigationCallbacks(
    val onWalletOpened: (String) -> Unit,
    val onRouteChanged: (AppRoute) -> Unit,
)

internal data class AppNavigationTarget(
    val route: Any,
    val routePattern: String,
    val graphRoute: String,
    val routeKey: String = routePattern,
)

internal class AppRouteNavigator {
    fun buildGraphRoute(route: AppRoute): Any {
        return when (route.routeDescriptor.graphRoute) {
            AuthGraphRoutePattern -> AuthGraphRoute
            else -> MainGraphRoute
        }
    }

    fun buildNavigationTarget(
        route: AppRoute,
        activeChatConversationId: String,
    ): AppNavigationTarget {
        return when (route) {
            AppRoute.AuthEntry -> AppNavigationTarget(LoginRoute, LoginRoutePattern, route.routeDescriptor.graphRoute)
            AppRoute.RegisterEntry -> AppNavigationTarget(RegisterRoute, RegisterRoutePattern, route.routeDescriptor.graphRoute)
            AppRoute.Inbox -> AppNavigationTarget(InboxRoute, InboxRoutePattern, route.routeDescriptor.graphRoute)
            AppRoute.Contacts -> AppNavigationTarget(ContactsRoute, ContactsRoutePattern, route.routeDescriptor.graphRoute)
            AppRoute.NewFriends -> AppNavigationTarget(NewFriendsRoute, NewFriendsRoutePattern, route.routeDescriptor.graphRoute)
            AppRoute.Wallet -> AppNavigationTarget(WalletRoute, WalletRoutePattern, route.routeDescriptor.graphRoute)
            AppRoute.Me -> AppNavigationTarget(MeRoute, MeRoutePattern, route.routeDescriptor.graphRoute)
            AppRoute.CallSession -> AppNavigationTarget(CallSessionRoute, CallSessionRoutePattern, route.routeDescriptor.graphRoute)
            AppRoute.Chat -> if (activeChatConversationId.isBlank()) {
                AppNavigationTarget(InboxRoute, InboxRoutePattern, route.routeDescriptor.graphRoute)
            } else {
                AppNavigationTarget(
                    route = ChatThreadRoute(activeChatConversationId),
                    routePattern = ChatThreadRoutePattern,
                    graphRoute = route.routeDescriptor.graphRoute,
                    routeKey = chatThreadRouteKey(activeChatConversationId),
                )
            }
        }
    }

    fun chatThreadRouteKey(conversationId: String): String {
        return "$ChatRoutePattern/$conversationId"
    }

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
        return route.routeDescriptor.shellDestination
    }

    fun resolveRouteShellDestination(
        route: AppRoute,
        hasActiveChatConversation: Boolean = true,
    ): ShellDestination {
        return when {
            route == AppRoute.NewFriends -> ShellDestination.Contacts
            route.routeDescriptor.requiresActiveChatConversation && !hasActiveChatConversation -> ShellDestination.Inbox
            else -> toShellDestination(route)
        }
    }
}

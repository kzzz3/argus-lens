package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.navigation.routeDescriptor
import com.kzzz3.argus.lens.app.navigation.routeString
import com.kzzz3.argus.lens.app.runtime.AppRouteNavigationCallbacks
import com.kzzz3.argus.lens.app.runtime.AppRouteNavigationRequest
import com.kzzz3.argus.lens.app.runtime.AppRouteNavigator
import com.kzzz3.argus.lens.feature.auth.navigation.AuthGraphRoute
import com.kzzz3.argus.lens.feature.auth.navigation.AuthGraphRoutePattern
import com.kzzz3.argus.lens.navigation.MainGraphRoute
import com.kzzz3.argus.lens.navigation.MainGraphRoutePattern
import com.kzzz3.argus.lens.navigation.TopLevelDestination
import com.kzzz3.argus.lens.ui.shell.ShellDestination
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRouteNavigatorTest {
    @Test
    fun appRouteDescriptorsExposeStableRouteStringsAndGraphMetadata() {
        assertEquals("AuthEntry", AppRoute.AuthEntry.routeString)
        assertEquals(AuthGraphRoutePattern, AppRoute.AuthEntry.routeDescriptor.graphRoute)
        assertEquals("RegisterEntry", AppRoute.RegisterEntry.routeString)
        assertEquals(AuthGraphRoutePattern, AppRoute.RegisterEntry.routeDescriptor.graphRoute)

        assertEquals("Inbox", AppRoute.Inbox.routeString)
        assertEquals(MainGraphRoutePattern, AppRoute.Inbox.routeDescriptor.graphRoute)
        assertEquals(ShellDestination.Inbox, AppRoute.Inbox.routeDescriptor.shellDestination)
        assertEquals("Chat", AppRoute.Chat.routeString)
        assertEquals(MainGraphRoutePattern, AppRoute.Chat.routeDescriptor.graphRoute)
        assertEquals(ShellDestination.Secondary, AppRoute.Chat.routeDescriptor.shellDestination)
        assertTrue(AppRoute.Chat.routeDescriptor.requiresActiveChatConversation)
    }

    @Test
    fun buildGraphRouteUsesStableRouteContractForEveryRoute() {
        val navigator = AppRouteNavigator()

        AppRoute.entries.forEach { route ->
            val expectedGraphRoute = when (route.routeDescriptor.graphRoute) {
                AuthGraphRoutePattern -> AuthGraphRoute
                else -> MainGraphRoute
            }
            assertEquals(expectedGraphRoute, navigator.buildGraphRoute(route))
        }
    }

    @Test
    fun openTopLevelRoute_walletOpensCurrentAccountBeforeRouting() {
        val navigator = AppRouteNavigator()
        val events = mutableListOf<String>()
        var openedWalletAccountId: String? = null
        var routedTo: AppRoute? = null

        navigator.openTopLevelRoute(
            route = AppRoute.Wallet,
            request = AppRouteNavigationRequest(
                accountId = "new-account",
            ),
            callbacks = AppRouteNavigationCallbacks(
                onWalletOpened = {
                    events += "wallet"
                    openedWalletAccountId = it
                },
                onRouteChanged = {
                    events += "route"
                    routedTo = it
                },
            ),
        )

        assertEquals(listOf("wallet", "route"), events)
        assertEquals("new-account", openedWalletAccountId)
        assertEquals(AppRoute.Wallet, routedTo)
    }

    @Test
    fun openTopLevelRoute_nonWalletRoutesWithoutChangingWallet() {
        val navigator = AppRouteNavigator()
        var openedWalletAccountId: String? = null
        var routedTo: AppRoute? = null

        navigator.openTopLevelRoute(
            route = AppRoute.Contacts,
            request = AppRouteNavigationRequest(
                accountId = "tester",
            ),
            callbacks = AppRouteNavigationCallbacks(
                onWalletOpened = { openedWalletAccountId = it },
                onRouteChanged = { routedTo = it },
            ),
        )

        assertNull(openedWalletAccountId)
        assertEquals(AppRoute.Contacts, routedTo)
    }

    @Test
    fun openShellDestination_secondaryDoesNothing() {
        val navigator = AppRouteNavigator()
        var routedTo: AppRoute? = null
        var openedWalletAccountId: String? = null

        navigator.openShellDestination(
            destination = ShellDestination.Secondary,
            request = AppRouteNavigationRequest(
                accountId = "tester",
            ),
            callbacks = AppRouteNavigationCallbacks(
                onWalletOpened = { openedWalletAccountId = it },
                onRouteChanged = { routedTo = it },
            ),
        )

        assertNull(openedWalletAccountId)
        assertNull(routedTo)
    }

    @Test
    fun openShellDestination_topLevelRoutesWithoutChangingWalletExceptWallet() {
        val navigator = AppRouteNavigator()
        val routes = listOf(
            ShellDestination.Inbox to AppRoute.Inbox,
            ShellDestination.Contacts to AppRoute.Contacts,
            ShellDestination.Me to AppRoute.Me,
        )

        routes.forEach { (destination, route) ->
            var routedTo: AppRoute? = null
            var openedWalletAccountId: String? = null

            navigator.openShellDestination(
                destination = destination,
                request = AppRouteNavigationRequest(
                    accountId = "tester",
                ),
                callbacks = AppRouteNavigationCallbacks(
                    onWalletOpened = { openedWalletAccountId = it },
                    onRouteChanged = { routedTo = it },
                ),
            )

            assertNull(openedWalletAccountId)
            assertEquals(route, routedTo)
        }
    }

    @Test
    fun openShellDestination_walletOpensCurrentAccountBeforeRouting() {
        val navigator = AppRouteNavigator()
        val events = mutableListOf<String>()
        var openedWalletAccountId: String? = null
        var routedTo: AppRoute? = null

        navigator.openShellDestination(
            destination = ShellDestination.Wallet,
            request = AppRouteNavigationRequest(
                accountId = "new-account",
            ),
            callbacks = AppRouteNavigationCallbacks(
                onWalletOpened = {
                    events += "wallet"
                    openedWalletAccountId = it
                },
                onRouteChanged = {
                    events += "route"
                    routedTo = it
                },
            ),
        )

        assertEquals(listOf("wallet", "route"), events)
        assertEquals("new-account", openedWalletAccountId)
        assertEquals(AppRoute.Wallet, routedTo)
    }

    @Test
    fun toShellDestination_mapsTopLevelAndSecondaryRoutes() {
        val navigator = AppRouteNavigator()

        assertEquals(ShellDestination.Inbox, navigator.toShellDestination(AppRoute.Inbox))
        assertEquals(ShellDestination.Contacts, navigator.toShellDestination(AppRoute.Contacts))
        assertEquals(ShellDestination.Wallet, navigator.toShellDestination(AppRoute.Wallet))
        assertEquals(ShellDestination.Me, navigator.toShellDestination(AppRoute.Me))
        assertEquals(ShellDestination.Secondary, navigator.toShellDestination(AppRoute.AuthEntry))
        assertEquals(ShellDestination.Secondary, navigator.toShellDestination(AppRoute.RegisterEntry))
        assertEquals(ShellDestination.Secondary, navigator.toShellDestination(AppRoute.NewFriends))
        assertEquals(ShellDestination.Secondary, navigator.toShellDestination(AppRoute.CallSession))
        assertEquals(ShellDestination.Secondary, navigator.toShellDestination(AppRoute.Chat))
    }

    @Test
    fun topLevelDestinationFromRouteMapsOnlyMainShellTabs() {
        assertEquals(TopLevelDestination.Inbox, TopLevelDestination.fromRoute(AppRoute.Inbox))
        assertEquals(TopLevelDestination.Contacts, TopLevelDestination.fromRoute(AppRoute.Contacts))
        assertEquals(TopLevelDestination.Wallet, TopLevelDestination.fromRoute(AppRoute.Wallet))
        assertEquals(TopLevelDestination.Me, TopLevelDestination.fromRoute(AppRoute.Me))
        assertNull(TopLevelDestination.fromRoute(AppRoute.AuthEntry))
        assertNull(TopLevelDestination.fromRoute(AppRoute.RegisterEntry))
        assertNull(TopLevelDestination.fromRoute(AppRoute.NewFriends))
        assertNull(TopLevelDestination.fromRoute(AppRoute.CallSession))
        assertNull(TopLevelDestination.fromRoute(AppRoute.Chat))
    }

    @Test
    fun topLevelDestinationFromShellDestinationMapsOnlyMainShellTabs() {
        assertEquals(TopLevelDestination.Inbox, TopLevelDestination.fromShellDestination(ShellDestination.Inbox))
        assertEquals(TopLevelDestination.Contacts, TopLevelDestination.fromShellDestination(ShellDestination.Contacts))
        assertEquals(TopLevelDestination.Wallet, TopLevelDestination.fromShellDestination(ShellDestination.Wallet))
        assertEquals(TopLevelDestination.Me, TopLevelDestination.fromShellDestination(ShellDestination.Me))
        assertNull(TopLevelDestination.fromShellDestination(ShellDestination.Secondary))
    }

    @Test
    fun resolveRouteShellDestination_keepsNewFriendsUnderContactsTab() {
        val navigator = AppRouteNavigator()

        assertEquals(ShellDestination.Contacts, navigator.resolveRouteShellDestination(AppRoute.NewFriends))
    }

    @Test
    fun resolveRouteShellDestination_usesInboxTabWhenChatConversationIsMissing() {
        val navigator = AppRouteNavigator()

        assertEquals(
            ShellDestination.Inbox,
            navigator.resolveRouteShellDestination(
                route = AppRoute.Chat,
                hasActiveChatConversation = false,
            ),
        )
    }

    @Test
    fun resolveRouteShellDestination_usesSecondaryTabWhenChatConversationExists() {
        val navigator = AppRouteNavigator()

        assertEquals(
            ShellDestination.Secondary,
            navigator.resolveRouteShellDestination(
                route = AppRoute.Chat,
                hasActiveChatConversation = true,
            ),
        )
    }

    @Test
    fun argusNavHostGraphs_registerEveryDeclaredAppRoute() {
        val authGraphSource = File("../feature/src/main/java/com/kzzz3/argus/lens/feature/auth/navigation/AuthNavigation.kt").readText()
        val mainGraphSources = mapOf(
            AppRoute.Inbox to File("../feature/src/main/java/com/kzzz3/argus/lens/feature/inbox/navigation/InboxNavigation.kt").readText(),
            AppRoute.Chat to File("../feature/src/main/java/com/kzzz3/argus/lens/feature/inbox/navigation/InboxNavigation.kt").readText(),
            AppRoute.Contacts to File("../feature/src/main/java/com/kzzz3/argus/lens/feature/contacts/navigation/ContactsNavigation.kt").readText(),
            AppRoute.NewFriends to File("../feature/src/main/java/com/kzzz3/argus/lens/feature/contacts/navigation/ContactsNavigation.kt").readText(),
            AppRoute.CallSession to File("../feature/src/main/java/com/kzzz3/argus/lens/feature/call/navigation/CallNavigation.kt").readText(),
            AppRoute.Wallet to File("../feature/src/main/java/com/kzzz3/argus/lens/feature/wallet/navigation/WalletNavigation.kt").readText(),
            AppRoute.Me to File("../feature/src/main/java/com/kzzz3/argus/lens/feature/me/navigation/MeNavigation.kt").readText(),
        )
        val missingRoutes = AppRoute.entries
            .filterNot { route ->
                when (route) {
                    AppRoute.AuthEntry -> authGraphSource.contains("data object LoginRoute")
                    AppRoute.RegisterEntry -> authGraphSource.contains("data object RegisterRoute")
                    AppRoute.Chat -> mainGraphSources.getValue(route).contains("data class ChatThreadRoute")
                    else -> mainGraphSources.getValue(route).contains("${route.name}Route")
                }
            }

        assertTrue("ArgusNavHost child graphs must register every AppRoute: $missingRoutes", missingRoutes.isEmpty())
    }
}

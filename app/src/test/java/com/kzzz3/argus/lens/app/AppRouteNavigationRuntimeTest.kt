package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.ui.shell.ShellDestination
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRouteNavigationRuntimeTest {
    @Test
    fun openTopLevelRoute_walletOpensCurrentAccountBeforeRouting() {
        val runtime = AppRouteNavigationRuntime()
        val events = mutableListOf<String>()
        var openedWalletAccountId: String? = null
        var routedTo: AppRoute? = null

        runtime.openTopLevelRoute(
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
        val runtime = AppRouteNavigationRuntime()
        var openedWalletAccountId: String? = null
        var routedTo: AppRoute? = null

        runtime.openTopLevelRoute(
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
        val runtime = AppRouteNavigationRuntime()
        var routedTo: AppRoute? = null
        var openedWalletAccountId: String? = null

        runtime.openShellDestination(
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
        val runtime = AppRouteNavigationRuntime()
        val routes = listOf(
            ShellDestination.Inbox to AppRoute.Inbox,
            ShellDestination.Contacts to AppRoute.Contacts,
            ShellDestination.Me to AppRoute.Me,
        )

        routes.forEach { (destination, route) ->
            var routedTo: AppRoute? = null
            var openedWalletAccountId: String? = null

            runtime.openShellDestination(
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
        val runtime = AppRouteNavigationRuntime()
        val events = mutableListOf<String>()
        var openedWalletAccountId: String? = null
        var routedTo: AppRoute? = null

        runtime.openShellDestination(
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
        val runtime = AppRouteNavigationRuntime()

        assertEquals(ShellDestination.Inbox, runtime.toShellDestination(AppRoute.Inbox))
        assertEquals(ShellDestination.Contacts, runtime.toShellDestination(AppRoute.Contacts))
        assertEquals(ShellDestination.Wallet, runtime.toShellDestination(AppRoute.Wallet))
        assertEquals(ShellDestination.Me, runtime.toShellDestination(AppRoute.Me))
        assertEquals(ShellDestination.Secondary, runtime.toShellDestination(AppRoute.AuthEntry))
        assertEquals(ShellDestination.Secondary, runtime.toShellDestination(AppRoute.RegisterEntry))
        assertEquals(ShellDestination.Secondary, runtime.toShellDestination(AppRoute.NewFriends))
        assertEquals(ShellDestination.Secondary, runtime.toShellDestination(AppRoute.CallSession))
        assertEquals(ShellDestination.Secondary, runtime.toShellDestination(AppRoute.Chat))
    }

    @Test
    fun resolveRouteShellDestination_keepsNewFriendsUnderContactsTab() {
        val runtime = AppRouteNavigationRuntime()

        assertEquals(ShellDestination.Contacts, runtime.resolveRouteShellDestination(AppRoute.NewFriends))
    }

    @Test
    fun resolveRouteShellDestination_usesInboxTabWhenChatConversationIsMissing() {
        val runtime = AppRouteNavigationRuntime()

        assertEquals(
            ShellDestination.Inbox,
            runtime.resolveRouteShellDestination(
                route = AppRoute.Chat,
                hasSelectedConversation = false,
            ),
        )
    }

    @Test
    fun resolveRouteShellDestination_usesSecondaryTabWhenChatConversationExists() {
        val runtime = AppRouteNavigationRuntime()

        assertEquals(
            ShellDestination.Secondary,
            runtime.resolveRouteShellDestination(
                route = AppRoute.Chat,
                hasSelectedConversation = true,
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
                    AppRoute.AuthEntry -> authGraphSource.contains("Login(\"${route.name}\")")
                    AppRoute.RegisterEntry -> authGraphSource.contains("Register(\"${route.name}\")")
                    else -> mainGraphSources.getValue(route).contains("\"${route.name}\"")
                }
            }

        assertTrue("ArgusNavHost child graphs must register every AppRoute: $missingRoutes", missingRoutes.isEmpty())
    }
}

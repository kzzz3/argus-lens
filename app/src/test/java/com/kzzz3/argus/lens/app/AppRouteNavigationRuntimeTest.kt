package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.ui.shell.ShellDestination
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRouteNavigationRuntimeTest {
    @Test
    fun openTopLevelRoute_walletBindsCurrentAccountBeforeRouting() {
        val runtime = AppRouteNavigationRuntime()
        val walletState = WalletState(
            currentAccountId = "old-account",
            hasAttemptedSummaryLoad = true,
        )
        var updatedWalletState: WalletState? = null
        var routedTo: AppRoute? = null

        runtime.openTopLevelRoute(
            route = AppRoute.Wallet,
            request = AppRouteNavigationRequest(
                accountId = "new-account",
                walletState = walletState,
            ),
            callbacks = AppRouteNavigationCallbacks(
                onWalletStateChanged = { updatedWalletState = it },
                onRouteChanged = { routedTo = it },
            ),
        )

        assertEquals("new-account", updatedWalletState?.currentAccountId)
        assertEquals(false, updatedWalletState?.hasAttemptedSummaryLoad)
        assertEquals(AppRoute.Wallet, routedTo)
    }

    @Test
    fun openTopLevelRoute_nonWalletRoutesWithoutChangingWallet() {
        val runtime = AppRouteNavigationRuntime()
        var updatedWalletState: WalletState? = null
        var routedTo: AppRoute? = null

        runtime.openTopLevelRoute(
            route = AppRoute.Contacts,
            request = AppRouteNavigationRequest(
                accountId = "tester",
                walletState = WalletState(),
            ),
            callbacks = AppRouteNavigationCallbacks(
                onWalletStateChanged = { updatedWalletState = it },
                onRouteChanged = { routedTo = it },
            ),
        )

        assertNull(updatedWalletState)
        assertEquals(AppRoute.Contacts, routedTo)
    }

    @Test
    fun openShellDestination_secondaryDoesNothing() {
        val runtime = AppRouteNavigationRuntime()
        var routedTo: AppRoute? = null
        var updatedWalletState: WalletState? = null

        runtime.openShellDestination(
            destination = ShellDestination.Secondary,
            request = AppRouteNavigationRequest(
                accountId = "tester",
                walletState = WalletState(),
            ),
            callbacks = AppRouteNavigationCallbacks(
                onWalletStateChanged = { updatedWalletState = it },
                onRouteChanged = { routedTo = it },
            ),
        )

        assertNull(updatedWalletState)
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
            var updatedWalletState: WalletState? = null

            runtime.openShellDestination(
                destination = destination,
                request = AppRouteNavigationRequest(
                    accountId = "tester",
                    walletState = WalletState(),
                ),
                callbacks = AppRouteNavigationCallbacks(
                    onWalletStateChanged = { updatedWalletState = it },
                    onRouteChanged = { routedTo = it },
                ),
            )

            assertNull(updatedWalletState)
            assertEquals(route, routedTo)
        }
    }

    @Test
    fun openShellDestination_walletBindsCurrentAccountBeforeRouting() {
        val runtime = AppRouteNavigationRuntime()
        val events = mutableListOf<String>()
        var updatedWalletState: WalletState? = null
        var routedTo: AppRoute? = null

        runtime.openShellDestination(
            destination = ShellDestination.Wallet,
            request = AppRouteNavigationRequest(
                accountId = "new-account",
                walletState = WalletState(
                    currentAccountId = "old-account",
                    hasAttemptedSummaryLoad = true,
                ),
            ),
            callbacks = AppRouteNavigationCallbacks(
                onWalletStateChanged = {
                    events += "wallet"
                    updatedWalletState = it
                },
                onRouteChanged = {
                    events += "route"
                    routedTo = it
                },
            ),
        )

        assertEquals(listOf("wallet", "route"), events)
        assertEquals("new-account", updatedWalletState?.currentAccountId)
        assertEquals(false, updatedWalletState?.hasAttemptedSummaryLoad)
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
    fun appRouteNavGraph_registersEveryDeclaredAppRoute() {
        val graphSource = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteNavGraph.kt").readText()
        val missingRoutes = AppRoute.entries
            .filterNot { route -> graphSource.contains("composable(AppRoute.${route.name}.name)") }

        assertTrue("AppRouteNavGraph must register every AppRoute: $missingRoutes", missingRoutes.isEmpty())
    }
}

package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.auth.navigation.AuthGraphRoutePattern
import com.kzzz3.argus.lens.navigation.MainGraphRoutePattern
import com.kzzz3.argus.lens.navigation.TopLevelDestination
import com.kzzz3.argus.lens.navigation.graphRouteForAppRoute
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationGraphBoundaryTest {
    @Test
    fun argusNavHostOwnsRootGraphWithAuthAndMainChildren() {
        val shellHostSource = appHostSource("AppShellHost.kt").readText()
        val navHostSource = navigationSource("ArgusNavHost.kt").readText()
        val mainNavigationSource = navigationSource("MainNavigation.kt").readText()

        assertTrue("AppShellHost should call the normalized root nav host", shellHostSource.contains("ArgusNavHost("))
        assertFalse("AppShellHost should not call the old flat graph", shellHostSource.contains("AppRouteNavGraph("))
        assertTrue(navHostSource.contains("NavHost("))
        assertTrue(navHostSource.contains("authGraph(routes.auth)"))
        assertTrue(navHostSource.contains("mainGraph(routes.main)"))
        assertTrue(navHostSource.contains("data class ArgusNavRoutes"))
        assertTrue(mainNavigationSource.contains("navigation<MainGraphRoute>"))
        listOf(
            "inboxNavigation(routes.inbox)",
            "contactsNavigation(routes.contacts)",
            "callNavigation(routes.call)",
            "walletNavigation(routes.wallet)",
            "meNavigation(routes.me)",
        ).forEach { expected ->
            assertTrue(mainNavigationSource.contains(expected))
        }
    }

    @Test
    fun authNavigationOwnsLoginAndRegisterChildRoutes() {
        val authNavigationSource = featureAuthNavigationSource("AuthNavigation.kt").readText()

        assertTrue(authNavigationSource.contains("navigation<AuthGraphRoute>"))
        assertTrue(authNavigationSource.contains("data object LoginRoute"))
        assertTrue(authNavigationSource.contains("data object RegisterRoute"))
        assertTrue(authNavigationSource.contains("data class AuthRoutes"))
        assertTrue(authNavigationSource.contains("AuthEntryScreen("))
        assertTrue(authNavigationSource.contains("RegisterScreen("))
    }

    @Test
    fun featureNavigationFilesOwnMainChildRouteRegistrations() {
        assertTrue(featureNavigationSource("inbox/navigation/InboxNavigation.kt").readText().contains("InboxScreen("))
        assertTrue(featureNavigationSource("inbox/navigation/InboxNavigation.kt").readText().contains("ChatScreen("))
        assertTrue(featureNavigationSource("contacts/navigation/ContactsNavigation.kt").readText().contains("ContactsScreen("))
        assertTrue(featureNavigationSource("contacts/navigation/ContactsNavigation.kt").readText().contains("NewFriendsScreen("))
        assertTrue(featureNavigationSource("call/navigation/CallNavigation.kt").readText().contains("CallSessionScreen("))
        assertTrue(featureNavigationSource("wallet/navigation/WalletNavigation.kt").readText().contains("WalletScreen("))
        assertTrue(featureNavigationSource("me/navigation/MeNavigation.kt").readText().contains("MeScreen("))
    }

    @Test
    fun topLevelDestinationOnlyContainsMainShellTabs() {
        val destinations = TopLevelDestination.entries.map { it.route }

        assertEquals(
            listOf(AppRoute.Inbox, AppRoute.Contacts, AppRoute.Wallet, AppRoute.Me),
            destinations,
        )
    }

    @Test
    fun graphRouteForAppRouteSeparatesAuthAndMainDestinations() {
        assertEquals(AuthGraphRoutePattern, graphRouteForAppRoute(AppRoute.AuthEntry))
        assertEquals(AuthGraphRoutePattern, graphRouteForAppRoute(AppRoute.RegisterEntry))
        listOf(
            AppRoute.Inbox,
            AppRoute.Contacts,
            AppRoute.NewFriends,
            AppRoute.Wallet,
            AppRoute.Me,
            AppRoute.CallSession,
            AppRoute.Chat,
        ).forEach { route ->
            assertEquals(MainGraphRoutePattern, graphRouteForAppRoute(route))
        }
    }

    @Test
    fun hostEffectsClearPreviousGraphWhenRouteCrossesNavigationAreas() {
        val effectsSource = appHostSource("AppShellEffects.kt").readText()

        assertTrue(effectsSource.contains("buildNavigationTarget("))
        assertTrue(effectsSource.contains("previousGraphRoute != targetNavigation.graphRoute"))
        assertTrue(effectsSource.contains("popUpTo(previousGraphRoute)"))
    }

    @Test
    fun hostEffectsNavigateWithRouteContractInsteadOfBareEnumName() {
        val effectsSource = appHostSource("AppShellEffects.kt").readText()

        assertTrue(effectsSource.contains("buildNavigationTarget("))
        assertFalse(effectsSource.contains("currentDestinationRoute != currentRoute.name"))
        assertFalse(effectsSource.contains("navController.navigate(currentRoute.name)"))
    }

    @Test
    fun mainGraphStartDestinationUsesStableRouteContract() {
        val mainNavigationSource = navigationSource("MainNavigation.kt").readText()

        assertTrue(mainNavigationSource.contains("startDestination = InboxRoute"))
        assertFalse(mainNavigationSource.contains("AppRoute.Inbox.name"))
    }

    private fun appHostSource(fileName: String): File = File("src/main/java/com/kzzz3/argus/lens/app/host/$fileName")

    private fun navigationSource(fileName: String): File = File("src/main/java/com/kzzz3/argus/lens/navigation/$fileName")

    private fun featureAuthNavigationSource(fileName: String): File = File("../feature/src/main/java/com/kzzz3/argus/lens/feature/auth/navigation/$fileName")

    private fun featureNavigationSource(relativePath: String): File = File("../feature/src/main/java/com/kzzz3/argus/lens/feature/$relativePath")
}

package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.auth.navigation.AuthGraphRoute
import com.kzzz3.argus.lens.navigation.MainGraphRoute
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
        val routeHostSource = appSource("AppRouteHost.kt").readText()
        val navHostSource = navigationSource("ArgusNavHost.kt").readText()
        val mainNavigationSource = navigationSource("MainNavigation.kt").readText()

        assertTrue("AppRouteHost should call the normalized root nav host", routeHostSource.contains("ArgusNavHost("))
        assertFalse("AppRouteHost should not call the old flat graph", routeHostSource.contains("AppRouteNavGraph("))
        assertTrue(navHostSource.contains("NavHost("))
        assertTrue(navHostSource.contains("authGraph("))
        assertTrue(navHostSource.contains("mainGraph("))
        assertTrue(mainNavigationSource.contains("navigation("))
        assertTrue(mainNavigationSource.contains("route = MainGraphRoute"))
        assertTrue(mainNavigationSource.contains("inboxNavigation("))
        assertTrue(mainNavigationSource.contains("contactsNavigation("))
        assertTrue(mainNavigationSource.contains("callNavigation("))
        assertTrue(mainNavigationSource.contains("walletNavigation("))
        assertTrue(mainNavigationSource.contains("meNavigation("))
    }

    @Test
    fun authNavigationOwnsLoginAndRegisterChildRoutes() {
        val authNavigationSource = featureAuthNavigationSource("AuthNavigation.kt").readText()

        assertTrue(authNavigationSource.contains("navigation("))
        assertTrue(authNavigationSource.contains("route = AuthGraphRoute"))
        assertTrue(authNavigationSource.contains("AuthDestination.Login.route"))
        assertTrue(authNavigationSource.contains("AuthDestination.Register.route"))
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
        assertEquals(AuthGraphRoute, graphRouteForAppRoute(AppRoute.AuthEntry))
        assertEquals(AuthGraphRoute, graphRouteForAppRoute(AppRoute.RegisterEntry))
        listOf(
            AppRoute.Inbox,
            AppRoute.Contacts,
            AppRoute.NewFriends,
            AppRoute.Wallet,
            AppRoute.Me,
            AppRoute.CallSession,
            AppRoute.Chat,
        ).forEach { route ->
            assertEquals(MainGraphRoute, graphRouteForAppRoute(route))
        }
    }

    @Test
    fun hostEffectsClearPreviousGraphWhenRouteCrossesNavigationAreas() {
        val effectsSource = appSource("AppRouteHostEffects.kt").readText()

        assertTrue(effectsSource.contains("graphRouteForAppRoute(currentRoute)"))
        assertTrue(effectsSource.contains("previousGraphRoute != targetGraphRoute"))
        assertTrue(effectsSource.contains("popUpTo(previousGraphRoute)"))
    }

    @Test
    fun hostEffectsNavigateWithRouteContractInsteadOfBareEnumName() {
        val effectsSource = appSource("AppRouteHostEffects.kt").readText()

        assertTrue(effectsSource.contains("buildNavigationRoute(currentRoute)"))
        assertFalse(effectsSource.contains("currentDestinationRoute != currentRoute.name"))
        assertFalse(effectsSource.contains("navController.navigate(currentRoute.name)"))
    }

    @Test
    fun mainGraphStartDestinationUsesStableRouteContract() {
        val mainNavigationSource = navigationSource("MainNavigation.kt").readText()

        assertTrue(mainNavigationSource.contains("AppRoute.Inbox.routeString"))
        assertFalse(mainNavigationSource.contains("AppRoute.Inbox.name"))
    }

    private fun appSource(fileName: String): File = File("src/main/java/com/kzzz3/argus/lens/app/$fileName")

    private fun navigationSource(fileName: String): File = File("src/main/java/com/kzzz3/argus/lens/navigation/$fileName")

    private fun featureAuthNavigationSource(fileName: String): File = File("../feature/src/main/java/com/kzzz3/argus/lens/feature/auth/navigation/$fileName")

    private fun featureNavigationSource(relativePath: String): File = File("../feature/src/main/java/com/kzzz3/argus/lens/feature/$relativePath")
}

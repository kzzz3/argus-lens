package com.kzzz3.argus.lens.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import com.kzzz3.argus.lens.feature.call.navigation.CallSessionRoutes
import com.kzzz3.argus.lens.feature.call.navigation.callNavigation
import com.kzzz3.argus.lens.feature.contacts.navigation.ContactsRoutes
import com.kzzz3.argus.lens.feature.contacts.navigation.contactsNavigation
import com.kzzz3.argus.lens.feature.inbox.navigation.InboxRoute
import com.kzzz3.argus.lens.feature.inbox.navigation.InboxRoutes
import com.kzzz3.argus.lens.feature.inbox.navigation.inboxNavigation
import com.kzzz3.argus.lens.feature.me.navigation.MeRoutes
import com.kzzz3.argus.lens.feature.me.navigation.meNavigation
import com.kzzz3.argus.lens.feature.wallet.navigation.WalletRoutes
import com.kzzz3.argus.lens.feature.wallet.navigation.walletNavigation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val MainGraphRoutePattern = "main_graph"

@Serializable
@SerialName(MainGraphRoutePattern)
data object MainGraphRoute

internal fun NavGraphBuilder.mainGraph(
    routes: MainRoutes,
) {
    navigation<MainGraphRoute>(
        startDestination = InboxRoute,
    ) {
        inboxNavigation(routes.inbox)
        contactsNavigation(routes.contacts)
        callNavigation(routes.call)
        walletNavigation(routes.wallet)
        meNavigation(routes.me)
    }
}

internal data class MainRoutes(
    val inbox: InboxRoutes,
    val contacts: ContactsRoutes,
    val call: CallSessionRoutes,
    val wallet: WalletRoutes,
    val me: MeRoutes,
)

package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.inbox.InboxAction
import org.junit.Assert.assertEquals
import org.junit.Test

class InboxActionRouteRuntimeTest {
    @Test
    fun handleAction_openConversationOpensRequestedConversationOnly() {
        val runtime = InboxActionRouteRuntime()
        val events = mutableListOf<String>()

        runtime.handleAction(
            action = InboxAction.OpenConversation("conversation-1"),
            callbacks = callbacks(
                openConversation = { events += "conversation:$it" },
                openTopLevelRoute = { events += "route:$it" },
                signOutToEntry = { events += "sign-out" },
            ),
        )

        assertEquals(listOf("conversation:conversation-1"), events)
    }

    @Test
    fun handleAction_openContactsAndWalletRouteToTopLevelDestinations() {
        val runtime = InboxActionRouteRuntime()
        val events = mutableListOf<String>()
        val routeCallbacks = callbacks(
            openConversation = { events += "conversation:$it" },
            openTopLevelRoute = { events += "route:$it" },
            signOutToEntry = { events += "sign-out" },
        )

        runtime.handleAction(
            action = InboxAction.OpenContacts,
            callbacks = routeCallbacks,
        )
        runtime.handleAction(
            action = InboxAction.OpenWallet,
            callbacks = routeCallbacks,
        )

        assertEquals(listOf("route:${AppRoute.Contacts}", "route:${AppRoute.Wallet}"), events)
    }

    @Test
    fun handleAction_signOutToHudSignsOutToEntryOnly() {
        val runtime = InboxActionRouteRuntime()
        val events = mutableListOf<String>()

        runtime.handleAction(
            action = InboxAction.SignOutToHud,
            callbacks = callbacks(
                openConversation = { events += "conversation:$it" },
                openTopLevelRoute = { events += "route:$it" },
                signOutToEntry = { events += "sign-out" },
            ),
        )

        assertEquals(listOf("sign-out"), events)
    }

    private fun callbacks(
        openConversation: (String) -> Unit = {},
        openTopLevelRoute: (AppRoute) -> Unit = {},
        signOutToEntry: () -> Unit = {},
    ): InboxActionRouteCallbacks {
        return InboxActionRouteCallbacks(
            openConversation = openConversation,
            openTopLevelRoute = openTopLevelRoute,
            signOutToEntry = signOutToEntry,
        )
    }
}

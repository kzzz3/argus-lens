package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.inbox.InboxAction

internal data class InboxActionRouteCallbacks(
    val openConversation: (String) -> Unit,
    val openTopLevelRoute: (AppRoute) -> Unit,
    val signOutToEntry: () -> Unit,
)

internal class InboxActionRouteRuntime {
    fun handleAction(
        action: InboxAction,
        callbacks: InboxActionRouteCallbacks,
    ) {
        when (action) {
            is InboxAction.OpenConversation -> callbacks.openConversation(action.conversationId)
            InboxAction.OpenContacts -> callbacks.openTopLevelRoute(AppRoute.Contacts)
            InboxAction.OpenWallet -> callbacks.openTopLevelRoute(AppRoute.Wallet)
            InboxAction.SignOutToHud -> callbacks.signOutToEntry()
        }
    }
}

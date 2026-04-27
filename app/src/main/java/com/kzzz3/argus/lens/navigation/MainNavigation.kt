package com.kzzz3.argus.lens.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import com.kzzz3.argus.lens.app.AppRouteNavigationRuntime
import com.kzzz3.argus.lens.app.AppRouteUiState
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.navigation.routeString
import com.kzzz3.argus.lens.feature.call.CallSessionAction
import com.kzzz3.argus.lens.feature.call.navigation.callNavigation
import com.kzzz3.argus.lens.feature.contacts.ContactsAction
import com.kzzz3.argus.lens.feature.contacts.NewFriendsAction
import com.kzzz3.argus.lens.feature.contacts.navigation.contactsNavigation
import com.kzzz3.argus.lens.feature.inbox.ChatAction
import com.kzzz3.argus.lens.feature.inbox.InboxAction
import com.kzzz3.argus.lens.feature.inbox.navigation.inboxNavigation
import com.kzzz3.argus.lens.feature.me.navigation.meNavigation
import com.kzzz3.argus.lens.feature.wallet.WalletAction
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.feature.wallet.navigation.walletNavigation
import com.kzzz3.argus.lens.ui.shell.ShellDestination

const val MainGraphRoute = "main_graph"

internal fun NavGraphBuilder.mainGraph(
    currentRoute: AppRoute,
    routeUiState: AppRouteUiState,
    walletStateModel: WalletState,
    appRouteNavigationRuntime: AppRouteNavigationRuntime,
    onShellDestinationSelected: (ShellDestination) -> Unit,
    onInboxAction: (InboxAction) -> Unit,
    onContactsAction: (ContactsAction) -> Unit,
    onNewFriendsAction: (NewFriendsAction) -> Unit,
    onCallSessionAction: (CallSessionAction) -> Unit,
    onWalletAction: (WalletAction) -> Unit,
    onSignOut: () -> Unit,
    onChatAction: (ChatAction) -> Unit,
) {
    navigation(
        startDestination = AppRoute.Inbox.routeString,
        route = MainGraphRoute,
    ) {
        inboxNavigation(
            inboxShellDestination = appRouteNavigationRuntime.toShellDestination(AppRoute.Inbox),
            chatShellDestination = appRouteNavigationRuntime.toShellDestination(currentRoute),
            missingChatShellDestination = appRouteNavigationRuntime.resolveRouteShellDestination(
                route = AppRoute.Chat,
                hasSelectedConversation = false,
            ),
            onTabSelected = onShellDestinationSelected,
            inboxState = routeUiState.inboxState,
            chatState = routeUiState.chatState,
            chatUiState = routeUiState.chatUiState,
            onInboxAction = onInboxAction,
            onChatAction = onChatAction,
        )
        contactsNavigation(
            contactsShellDestination = appRouteNavigationRuntime.toShellDestination(AppRoute.Contacts),
            newFriendsShellDestination = appRouteNavigationRuntime.resolveRouteShellDestination(AppRoute.NewFriends),
            onTabSelected = onShellDestinationSelected,
            contactsState = routeUiState.contactsUiState,
            newFriendsState = routeUiState.newFriendsUiState,
            onContactsAction = onContactsAction,
            onNewFriendsAction = onNewFriendsAction,
        )
        callNavigation(
            callShellDestination = appRouteNavigationRuntime.toShellDestination(currentRoute),
            onTabSelected = onShellDestinationSelected,
            callSessionState = routeUiState.callSessionUiState,
            onCallSessionAction = onCallSessionAction,
        )
        walletNavigation(
            walletShellDestination = appRouteNavigationRuntime.toShellDestination(AppRoute.Wallet),
            onTabSelected = onShellDestinationSelected,
            walletState = routeUiState.walletUiState,
            permissionRequestPending = walletStateModel.shouldRequestCameraPermission,
            onWalletAction = onWalletAction,
        )
        meNavigation(
            meShellDestination = appRouteNavigationRuntime.toShellDestination(AppRoute.Me),
            onTabSelected = onShellDestinationSelected,
            meState = routeUiState.meUiState,
            onSignOut = onSignOut,
        )
    }
}

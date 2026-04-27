package com.kzzz3.argus.lens.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.kzzz3.argus.lens.app.AppRouteNavigationRuntime
import com.kzzz3.argus.lens.app.AppRouteUiState
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.navigation.graphRoute
import com.kzzz3.argus.lens.feature.auth.AuthEntryAction
import com.kzzz3.argus.lens.feature.auth.navigation.authGraph
import com.kzzz3.argus.lens.feature.call.CallSessionAction
import com.kzzz3.argus.lens.feature.contacts.ContactsAction
import com.kzzz3.argus.lens.feature.contacts.NewFriendsAction
import com.kzzz3.argus.lens.feature.inbox.ChatAction
import com.kzzz3.argus.lens.feature.inbox.InboxAction
import com.kzzz3.argus.lens.feature.register.RegisterAction
import com.kzzz3.argus.lens.feature.wallet.WalletAction
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.ui.shell.ShellDestination

@Composable
internal fun ArgusNavHost(
    navController: NavHostController,
    startDestination: String,
    currentRoute: AppRoute,
    routeUiState: AppRouteUiState,
    walletStateModel: WalletState,
    appRouteNavigationRuntime: AppRouteNavigationRuntime,
    onShellDestinationSelected: (ShellDestination) -> Unit,
    onAuthAction: (AuthEntryAction) -> Unit,
    onRegisterAction: (RegisterAction) -> Unit,
    onInboxAction: (InboxAction) -> Unit,
    onContactsAction: (ContactsAction) -> Unit,
    onNewFriendsAction: (NewFriendsAction) -> Unit,
    onCallSessionAction: (CallSessionAction) -> Unit,
    onWalletAction: (WalletAction) -> Unit,
    onSignOut: () -> Unit,
    onChatAction: (ChatAction) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        authGraph(
            authState = routeUiState.authState,
            registerState = routeUiState.registerState,
            onAuthAction = onAuthAction,
            onRegisterAction = onRegisterAction,
        )
        mainGraph(
            currentRoute = currentRoute,
            routeUiState = routeUiState,
            walletStateModel = walletStateModel,
            appRouteNavigationRuntime = appRouteNavigationRuntime,
            onShellDestinationSelected = onShellDestinationSelected,
            onInboxAction = onInboxAction,
            onContactsAction = onContactsAction,
            onNewFriendsAction = onNewFriendsAction,
            onCallSessionAction = onCallSessionAction,
            onWalletAction = onWalletAction,
            onSignOut = onSignOut,
            onChatAction = onChatAction,
        )
    }
}

internal fun graphRouteForAppRoute(route: AppRoute): String {
    return route.graphRoute
}

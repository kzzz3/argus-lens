package com.kzzz3.argus.lens.app

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.auth.AuthEntryAction
import com.kzzz3.argus.lens.feature.auth.AuthEntryScreen
import com.kzzz3.argus.lens.feature.call.CallSessionAction
import com.kzzz3.argus.lens.feature.call.CallSessionScreen
import com.kzzz3.argus.lens.feature.contacts.ContactsAction
import com.kzzz3.argus.lens.feature.contacts.ContactsScreen
import com.kzzz3.argus.lens.feature.contacts.NewFriendsAction
import com.kzzz3.argus.lens.feature.contacts.NewFriendsScreen
import com.kzzz3.argus.lens.feature.inbox.ChatAction
import com.kzzz3.argus.lens.feature.inbox.ChatScreen
import com.kzzz3.argus.lens.feature.inbox.InboxAction
import com.kzzz3.argus.lens.feature.inbox.InboxScreen
import com.kzzz3.argus.lens.feature.me.MeScreen
import com.kzzz3.argus.lens.feature.register.RegisterAction
import com.kzzz3.argus.lens.feature.register.RegisterScreen
import com.kzzz3.argus.lens.feature.wallet.WalletAction
import com.kzzz3.argus.lens.feature.wallet.WalletScreen
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.ui.shell.AuthenticatedShell
import com.kzzz3.argus.lens.ui.shell.ShellDestination

@Composable
internal fun AppRouteNavGraph(
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
        composable(AppRoute.AuthEntry.name) {
            AuthEntryScreen(
                state = routeUiState.authState,
                onAction = onAuthAction,
            )
        }

        composable(AppRoute.RegisterEntry.name) {
            RegisterScreen(
                state = routeUiState.registerState,
                onAction = onRegisterAction,
            )
        }

        composable(AppRoute.Inbox.name) {
            AuthenticatedRouteShell(
                currentDestination = appRouteNavigationRuntime.toShellDestination(currentRoute),
                onTabSelected = onShellDestinationSelected,
            ) { contentModifier ->
                InboxScreen(
                    state = routeUiState.inboxState,
                    onAction = onInboxAction,
                    modifier = contentModifier,
                )
            }
        }

        composable(AppRoute.Contacts.name) {
            AuthenticatedRouteShell(
                currentDestination = appRouteNavigationRuntime.toShellDestination(currentRoute),
                onTabSelected = onShellDestinationSelected,
            ) { contentModifier ->
                ContactsScreen(
                    state = routeUiState.contactsUiState,
                    onAction = onContactsAction,
                    modifier = contentModifier,
                )
            }
        }

        composable(AppRoute.NewFriends.name) {
            AuthenticatedRouteShell(
                currentDestination = appRouteNavigationRuntime.resolveRouteShellDestination(AppRoute.NewFriends),
                onTabSelected = onShellDestinationSelected,
            ) { contentModifier ->
                NewFriendsScreen(
                    state = routeUiState.newFriendsUiState,
                    onAction = onNewFriendsAction,
                    modifier = contentModifier,
                )
            }
        }

        composable(AppRoute.CallSession.name) {
            AuthenticatedRouteShell(
                currentDestination = appRouteNavigationRuntime.toShellDestination(currentRoute),
                onTabSelected = onShellDestinationSelected,
            ) { contentModifier ->
                CallSessionScreen(
                    state = routeUiState.callSessionUiState,
                    onAction = onCallSessionAction,
                    modifier = contentModifier,
                )
            }
        }

        composable(AppRoute.Wallet.name) {
            AuthenticatedRouteShell(
                currentDestination = appRouteNavigationRuntime.toShellDestination(currentRoute),
                onTabSelected = onShellDestinationSelected,
            ) { contentModifier ->
                WalletScreen(
                    state = routeUiState.walletUiState,
                    permissionRequestPending = walletStateModel.shouldRequestCameraPermission,
                    onAction = onWalletAction,
                    modifier = contentModifier,
                )
            }
        }

        composable(AppRoute.Me.name) {
            AuthenticatedRouteShell(
                currentDestination = appRouteNavigationRuntime.toShellDestination(currentRoute),
                onTabSelected = onShellDestinationSelected,
            ) { contentModifier ->
                MeScreen(
                    state = routeUiState.meUiState,
                    onSignOut = onSignOut,
                    modifier = contentModifier,
                )
            }
        }

        composable(AppRoute.Chat.name) {
            val resolvedChatUiState = routeUiState.chatUiState
            val resolvedChatState = routeUiState.chatState

            if (resolvedChatUiState == null || resolvedChatState == null) {
                AuthenticatedRouteShell(
                    currentDestination = appRouteNavigationRuntime.resolveRouteShellDestination(
                        route = AppRoute.Chat,
                        hasSelectedConversation = false,
                    ),
                    onTabSelected = onShellDestinationSelected,
                ) { contentModifier ->
                    InboxScreen(
                        state = routeUiState.inboxState,
                        onAction = onInboxAction,
                        modifier = contentModifier,
                    )
                }
            } else {
                AuthenticatedRouteShell(
                    currentDestination = appRouteNavigationRuntime.toShellDestination(currentRoute),
                    onTabSelected = onShellDestinationSelected,
                ) { contentModifier ->
                    ChatScreen(
                        state = resolvedChatUiState,
                        onAction = onChatAction,
                        modifier = contentModifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedRouteShell(
    currentDestination: ShellDestination,
    onTabSelected: (ShellDestination) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    AuthenticatedShell(
        currentDestination = currentDestination,
        onTabSelected = onTabSelected,
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}

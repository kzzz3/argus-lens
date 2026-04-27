package com.kzzz3.argus.lens.feature.contacts.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kzzz3.argus.lens.feature.contacts.ContactsAction
import com.kzzz3.argus.lens.feature.contacts.ContactsScreen
import com.kzzz3.argus.lens.feature.contacts.ContactsUiState
import com.kzzz3.argus.lens.feature.contacts.NewFriendsAction
import com.kzzz3.argus.lens.feature.contacts.NewFriendsScreen
import com.kzzz3.argus.lens.feature.contacts.NewFriendsUiState
import com.kzzz3.argus.lens.feature.navigation.AuthenticatedFeatureRouteShell
import com.kzzz3.argus.lens.ui.shell.ShellDestination

const val ContactsRoute = "Contacts"
const val NewFriendsRoute = "NewFriends"

fun NavGraphBuilder.contactsNavigation(
    contactsShellDestination: ShellDestination,
    newFriendsShellDestination: ShellDestination,
    onTabSelected: (ShellDestination) -> Unit,
    contactsState: ContactsUiState,
    newFriendsState: NewFriendsUiState,
    onContactsAction: (ContactsAction) -> Unit,
    onNewFriendsAction: (NewFriendsAction) -> Unit,
) {
    composable(ContactsRoute) {
        AuthenticatedFeatureRouteShell(
            currentDestination = contactsShellDestination,
            onTabSelected = onTabSelected,
        ) { contentModifier ->
            ContactsScreen(
                state = contactsState,
                onAction = onContactsAction,
                modifier = contentModifier,
            )
        }
    }

    composable(NewFriendsRoute) {
        AuthenticatedFeatureRouteShell(
            currentDestination = newFriendsShellDestination,
            onTabSelected = onTabSelected,
        ) { contentModifier ->
            NewFriendsScreen(
                state = newFriendsState,
                onAction = onNewFriendsAction,
                modifier = contentModifier,
            )
        }
    }
}

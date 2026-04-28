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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val ContactsRoutePattern = "Contacts"
const val NewFriendsRoutePattern = "NewFriends"

@Serializable
@SerialName(ContactsRoutePattern)
data object ContactsRoute

@Serializable
@SerialName(NewFriendsRoutePattern)
data object NewFriendsRoute

data class ContactsRoutes(
    val contactsShellDestination: ShellDestination,
    val newFriendsShellDestination: ShellDestination,
    val onTabSelected: (ShellDestination) -> Unit,
    val contactsState: ContactsUiState,
    val newFriendsState: NewFriendsUiState,
    val onContactsAction: (ContactsAction) -> Unit,
    val onNewFriendsAction: (NewFriendsAction) -> Unit,
)

fun NavGraphBuilder.contactsNavigation(
    routes: ContactsRoutes,
) {
    composable<ContactsRoute> {
        AuthenticatedFeatureRouteShell(
            currentDestination = routes.contactsShellDestination,
            onTabSelected = routes.onTabSelected,
        ) { contentModifier ->
            ContactsScreen(
                state = routes.contactsState,
                onAction = routes.onContactsAction,
                modifier = contentModifier,
            )
        }
    }

    composable<NewFriendsRoute> {
        AuthenticatedFeatureRouteShell(
            currentDestination = routes.newFriendsShellDestination,
            onTabSelected = routes.onTabSelected,
        ) { contentModifier ->
            NewFriendsScreen(
                state = routes.newFriendsState,
                onAction = routes.onNewFriendsAction,
                modifier = contentModifier,
            )
        }
    }
}

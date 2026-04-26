package com.kzzz3.argus.lens.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kzzz3.argus.lens.ui.theme.ImAvatarBlue
import com.kzzz3.argus.lens.ui.theme.ImAvatarMint
import com.kzzz3.argus.lens.ui.theme.ImBackground
import com.kzzz3.argus.lens.ui.theme.ImBackgroundElevated
import com.kzzz3.argus.lens.ui.theme.ImDivider
import com.kzzz3.argus.lens.ui.theme.ImGreen
import com.kzzz3.argus.lens.ui.theme.ImSurfaceElevated
import com.kzzz3.argus.lens.ui.theme.ImTextPrimary
import com.kzzz3.argus.lens.ui.theme.ImTextSecondary

private data class TopLevelDestination(
    val destination: ShellDestination,
    val label: String,
    val icon: ImageVector,
)

enum class ShellDestination {
    Inbox,
    Contacts,
    Wallet,
    Me,
    Secondary,
}

private val TopLevelDestinations = listOf(
    TopLevelDestination(ShellDestination.Inbox, "Chats", Icons.Rounded.ChatBubble),
    TopLevelDestination(ShellDestination.Contacts, "Contacts", Icons.Rounded.People),
    TopLevelDestination(ShellDestination.Wallet, "Wallet", Icons.Rounded.AccountBalanceWallet),
    TopLevelDestination(ShellDestination.Me, "Me", Icons.Rounded.Person),
)

@Composable
fun AuthenticatedShell(
    currentDestination: ShellDestination,
    onTabSelected: (ShellDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val showBottomBar = currentDestination in TopLevelDestinations.map { it.destination }.toSet()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ImBackground),
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                if (showBottomBar) {
                    BottomNavigationBar(
                        currentDestination = currentDestination,
                        onTabSelected = onTabSelected,
                    )
                }
            },
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}

@Composable
private fun BottomNavigationBar(
    currentDestination: ShellDestination,
    onTabSelected: (ShellDestination) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(28.dp),
        color = ImSurfaceElevated.copy(alpha = 0.96f),
        shadowElevation = 18.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TopLevelDestinations.forEach { destination ->
                val selected = currentDestination == destination.destination
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(destination.destination) },
                    shape = RoundedCornerShape(22.dp),
                    color = if (selected) ImGreen.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (selected) ImGreen else if (destination.destination == ShellDestination.Wallet) ImAvatarMint else ImAvatarBlue,
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                tint = ImTextPrimary,
                            )
                        }
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) ImTextPrimary else ImTextSecondary,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (selected) ImGreen else ImDivider,
                        ) {
                            Spacer(modifier = Modifier.width(18.dp).height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

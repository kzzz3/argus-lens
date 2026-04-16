package com.kzzz3.argus.lens.feature.me

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kzzz3.argus.lens.ui.theme.ArguslensTheme
import com.kzzz3.argus.lens.ui.theme.ImAvatarMint
import com.kzzz3.argus.lens.ui.theme.ImBlue
import com.kzzz3.argus.lens.ui.theme.ImGreen
import com.kzzz3.argus.lens.ui.theme.ImSurfaceElevated
import com.kzzz3.argus.lens.ui.theme.ImTextMuted
import com.kzzz3.argus.lens.ui.theme.ImTextPrimary
import com.kzzz3.argus.lens.ui.theme.ImTextSecondary
import com.kzzz3.argus.lens.ui.shell.AppTopHeader

data class MeUiState(
    val displayName: String,
    val accountId: String,
    val walletSummary: String,
    val statusLabel: String,
    val summaryLine: String,
    val cards: List<MeStatCardUi>,
)

data class MeStatCardUi(
    val title: String,
    val value: String,
    val supporting: String,
)

@Composable
fun MeScreen(
    state: MeUiState,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        AppTopHeader(title = "Me") {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = ImGreen.copy(alpha = 0.18f),
                ) {
                    Text(
                        text = state.statusLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = ImTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = ImSurfaceElevated.copy(alpha = 0.94f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ImAvatarMint,
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = state.displayName.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = ImTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = state.displayName,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = ImTextPrimary,
                                    )
                                    Text(
                                        text = "Argus ID: ${state.accountId}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ImTextSecondary,
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = state.walletSummary,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ImTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = state.summaryLine,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ImTextSecondary,
                                )
                            }
                        }
                    }
                }
            }

            items(state.cards) { card ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ImSurfaceElevated.copy(alpha = 0.92f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = card.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = ImTextPrimary,
                            )
                            Text(
                                text = card.supporting,
                                style = MaterialTheme.typography.bodySmall,
                                color = ImTextMuted,
                            )
                        }
                        Text(
                            text = card.value,
                            style = MaterialTheme.typography.titleMedium,
                            color = ImTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Sign out")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MeScreenPreview() {
    ArguslensTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            MeScreen(
                state = MeUiState(
                    displayName = "Argus Tester",
                    accountId = "argus-tester",
                    walletSummary = "Wallet balance · CNY 1000.00",
                    statusLabel = "Online",
                    summaryLine = "Your IM shell is synced and ready for messaging, contacts, and wallet transfers.",
                    cards = listOf(
                        MeStatCardUi("Chats", "12 active threads", "Conversation list, realtime delivery, and local-first rendering are online."),
                        MeStatCardUi("Contacts", "8 saved friends", "Friend graph is loaded from the remote account service and linked to direct chats."),
                    ),
                ),
                onSignOut = {},
            )
        }
    }
}

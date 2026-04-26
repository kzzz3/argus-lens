package com.kzzz3.argus.lens.feature.wallet

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kzzz3.argus.lens.feature.scan.QrScannerPreview
import com.kzzz3.argus.lens.feature.scan.cameraPermissionName
import com.kzzz3.argus.lens.feature.scan.hasCameraPermission
import com.kzzz3.argus.lens.ui.theme.ImBlue
import com.kzzz3.argus.lens.ui.theme.ImGold
import com.kzzz3.argus.lens.ui.theme.ImGreen
import com.kzzz3.argus.lens.ui.theme.ImSurfaceElevated
import com.kzzz3.argus.lens.ui.theme.ImTextMuted
import com.kzzz3.argus.lens.ui.theme.ImTextPrimary
import com.kzzz3.argus.lens.ui.theme.ImTextSecondary
import com.kzzz3.argus.lens.ui.shell.AppTopHeader

private val WalletPanelColor = ImSurfaceElevated.copy(alpha = 0.94f)
private val WalletPrimaryAccent = ImGreen
private val WalletPositiveAccent = ImGreen
private val WalletNegativeAccent = Color(0xFFFF8A65)
private val WalletSecondaryText = ImTextSecondary
private val WalletMutedText = ImTextMuted

@Composable
fun WalletScreen(
    state: WalletUiState,
    permissionRequestPending: Boolean,
    onAction: (WalletAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        onAction(WalletAction.CameraPermissionResult(granted))
    }

    LaunchedEffect(permissionRequestPending) {
        if (permissionRequestPending) {
            cameraPermissionLauncher.launch(cameraPermissionName())
        }
    }

    LaunchedEffect(Unit) {
        val granted = hasCameraPermission(
            ContextCompat.checkSelfPermission(context, cameraPermissionName())
        )
        if (granted != state.cameraPermissionGranted) {
            onAction(WalletAction.CameraPermissionResult(granted))
        }
    }

    LaunchedEffect(state.shouldLoadSummary) {
        if (state.shouldLoadSummary) {
            onAction(WalletAction.RefreshWalletSummary)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        WalletHeader(state, onAction)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WalletStatusBanner(state)

            when (state.page) {
                WalletPage.Overview -> WalletOverviewPanel(state, onAction)
                WalletPage.PayScanner -> {
                    WalletScannerPanel(state, onAction)
                    WalletManualPanel(state, onAction)
                }
                WalletPage.PayReview -> WalletReviewPanel(state, onAction)
                WalletPage.PayResult -> WalletResultPanel(state, onAction)
                WalletPage.Collect -> WalletCollectPanel(state, onAction)
                WalletPage.History -> WalletHistoryPanel(state, onAction)
                WalletPage.ReceiptDetail -> WalletReceiptPanel(state, onAction)
            }

            if (state.page != WalletPage.Overview) {
                Button(
                    onClick = { onAction(WalletAction.NavigateBack) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = state.backActionLabel)
                }
            }
        }
    }
}

@Composable
private fun WalletHeader(state: WalletUiState, onAction: (WalletAction) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }

    AppTopHeader(title = state.title) {
        Box {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
            ) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = "Wallet actions",
                        tint = ImTextPrimary,
                    )
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("History") },
                    onClick = {
                        menuExpanded = false
                        onAction(WalletAction.OpenTransactionHistory)
                    },
                )
                DropdownMenuItem(
                    text = { Text(state.refreshActionLabel) },
                    onClick = {
                        menuExpanded = false
                        onAction(WalletAction.RefreshWalletSummary)
                    },
                )
            }
        }
    }
}

@Composable
private fun WalletStatusBanner(state: WalletUiState) {
    state.statusMessage?.let { message ->
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (state.isStatusError) MaterialTheme.colorScheme.errorContainer else WalletPrimaryAccent.copy(alpha = 0.16f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = ImTextPrimary,
            )
        }
    }
}

@Composable
private fun WalletOverviewPanel(state: WalletUiState, onAction: (WalletAction) -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = WalletPanelColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WalletSummaryHero(summary = state.summary, isLoading = state.isLoadingSummary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WalletActionCard(
                    title = state.payActionLabel,
                    subtitle = "Scan a collect QR code and send money out of this wallet.",
                    highlightColor = WalletPrimaryAccent,
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(WalletAction.OpenPayScanner) },
                )
                WalletActionCard(
                    title = state.collectActionLabel,
                    subtitle = "Render your own QR code so someone else can transfer to you.",
                    highlightColor = ImBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(WalletAction.OpenCollectQr) },
                )
            }
        }
    }
}

@Composable
private fun WalletScannerPanel(state: WalletUiState, onAction: (WalletAction) -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = WalletPanelColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WalletSectionIntro(
                title = "Scan to pay",
                subtitle = "Point the camera at another user's collect QR code to open a transfer review.",
            )
            if (!state.cameraPermissionGranted) {
                Text(
                    text = "Grant camera access to scan another user's payment code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalletSecondaryText,
                )
                Button(
                    onClick = { onAction(WalletAction.RequestCameraPermission) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = state.permissionActionLabel)
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth()) {
                    QrScannerPreview(
                        isActive = state.isScannerActive,
                        onPayloadDetected = { onAction(WalletAction.DetectedPayload(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                    )
                }
                Text(
                    text = state.scannerHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = WalletMutedText,
                )
            }
        }
    }
}

@Composable
private fun WalletManualPanel(state: WalletUiState, onAction: (WalletAction) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = WalletPanelColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WalletSectionIntro(
                title = "Manual fallback",
                subtitle = "If the QR payload was copied into chat, paste it here and resolve the transfer request manually.",
            )
            OutlinedTextField(
                value = state.manualPayload,
                onValueChange = { onAction(WalletAction.UpdateManualPayload(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = state.manualPayloadLabel) },
                placeholder = { Text(text = state.manualPayloadPlaceholder) },
            )
            Button(
                onClick = { onAction(WalletAction.SubmitManualPayload) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = state.resolveActionLabel)
            }
        }
    }
}

@Composable
private fun WalletReviewPanel(state: WalletUiState, onAction: (WalletAction) -> Unit) {
    val resolution = state.resolution ?: return
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, WalletPrimaryAccent.copy(alpha = 0.24f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WalletDirectionBadge(text = "Recipient")
                WalletDirectionBadge(
                    text = if (resolution.amountEditable) "Editable amount" else "Fixed amount",
                )
            }
            Text(
                text = resolution.recipientDisplayName,
                style = MaterialTheme.typography.titleLarge,
                color = ImTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Recipient account: ${resolution.recipientAccountId}",
                style = MaterialTheme.typography.bodyMedium,
                color = WalletSecondaryText,
            )
            resolution.requestedAmount?.let { amount ->
                Text(
                    text = "Requested amount: ${resolution.currency} $amount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalletSecondaryText,
                )
            }
            OutlinedTextField(
                value = state.payAmountDraft,
                onValueChange = { onAction(WalletAction.UpdatePayAmountDraft(it)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = resolution.amountEditable,
                label = { Text(text = state.amountLabel) },
            )
            OutlinedTextField(
                value = state.payNoteDraft,
                onValueChange = { onAction(WalletAction.UpdatePayNoteDraft(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = state.noteLabel) },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onAction(WalletAction.ConfirmPayment) },
                    enabled = state.canConfirm,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = state.confirmActionLabel)
                }
                Button(
                    onClick = { onAction(WalletAction.ResetForRescan) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = state.rescanActionLabel)
                }
            }
        }
    }
}

@Composable
private fun WalletResultPanel(state: WalletUiState, onAction: (WalletAction) -> Unit) {
    val receipt = state.completedPayment ?: return
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, WalletPrimaryAccent.copy(alpha = 0.24f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${receipt.direction.name.lowercase().replaceFirstChar { it.uppercase() }} · ${receipt.currency} ${receipt.amount}",
                style = MaterialTheme.typography.titleLarge,
                color = ImTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (receipt.direction == WalletTransferDirection.Sent) {
                    "To ${receipt.recipientDisplayName}"
                } else {
                    "From ${receipt.payerDisplayName}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = WalletSecondaryText,
            )
            WalletDirectionBadge(
                text = if (receipt.direction == WalletTransferDirection.Sent) "Money sent" else "Money received",
                accentColor = if (receipt.direction == WalletTransferDirection.Sent) WalletNegativeAccent else WalletPositiveAccent,
            )
            Text(
                text = "Status: ${receipt.status}",
                style = MaterialTheme.typography.bodyMedium,
                color = WalletSecondaryText,
            )
            Text(
                text = "Processed at: ${receipt.paidAt}",
                style = MaterialTheme.typography.bodySmall,
                color = WalletMutedText,
            )
            val viewerBalance = if (receipt.direction == WalletTransferDirection.Sent) {
                receipt.payerBalanceAfter
            } else {
                receipt.recipientBalanceAfter
            }
            WalletStatStrip(
                primaryLabel = "Transfer amount",
                primaryValue = "${receipt.currency} ${receipt.amount}",
                secondaryLabel = "Balance after",
                secondaryValue = "${receipt.currency} $viewerBalance",
            )
            if (receipt.note.isNotBlank()) {
                Text(
                    text = "Note: ${receipt.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = WalletMutedText,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onAction(WalletAction.OpenTransactionHistory) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = state.openHistoryActionLabel)
                }
                Button(
                    onClick = { onAction(WalletAction.ResetForRescan) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = state.rescanActionLabel)
                }
            }
        }
    }
}

@Composable
private fun WalletCollectPanel(state: WalletUiState, onAction: (WalletAction) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = WalletPanelColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WalletSectionIntro(
                title = "Your collection QR",
                subtitle = "Share this code with another Argus user. They scan it from Pay and confirm the transfer on their device.",
            )
            Text(
                text = "Collect with your wallet QR",
                style = MaterialTheme.typography.titleLarge,
                color = ImTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            state.summary?.let { summary ->
                ReceiptLine("Account", summary.accountId)
                ReceiptLine("Display name", summary.displayName)
                ReceiptLine("Current balance", "${summary.currency} ${summary.balance}")
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(3.dp, Color(0x1A9AD0FF)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .aspectRatio(1f),
                ) {
                    WalletQrCodeImage(
                        payload = state.collectPayload,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            OutlinedTextField(
                value = state.collectAmountDraft,
                onValueChange = { onAction(WalletAction.UpdateCollectAmountDraft(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = state.collectAmountLabel) },
            )
            OutlinedTextField(
                value = state.collectNoteDraft,
                onValueChange = { onAction(WalletAction.UpdateCollectNoteDraft(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = state.collectNoteLabel) },
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0x0FAAC9E3),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Request preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = WalletMutedText,
                    )
                    Text(
                        text = when {
                            state.collectPayload.isBlank() -> "Wallet summary is still loading."
                            state.collectAmountDraft.isBlank() && state.collectNoteDraft.isBlank() -> "Open amount — payer will decide how much to send."
                            state.collectAmountDraft.isBlank() -> "Open amount with note: ${state.collectNoteDraft}"
                            state.collectNoteDraft.isBlank() -> "Fixed request for CNY ${state.collectAmountDraft}"
                            else -> "Fixed request for CNY ${state.collectAmountDraft} with note: ${state.collectNoteDraft}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = ImTextPrimary,
                    )
                    Text(
                        text = state.collectPayload.ifBlank { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        color = WalletSecondaryText,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onAction(WalletAction.ClearCollectRequest) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = state.clearCollectActionLabel)
                }
                Button(
                    onClick = { onAction(WalletAction.RefreshWalletSummary) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = state.refreshActionLabel)
                }
            }
        }
    }
}

@Composable
private fun WalletHistoryPanel(state: WalletUiState, onAction: (WalletAction) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = WalletPanelColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Transfer history",
                style = MaterialTheme.typography.titleLarge,
                color = ImTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            if (state.isHistoryLoading) {
                WalletInlineStatusCard(
                    title = "Loading transfer history",
                    supporting = "Checking the network and cached records for your latest wallet activity.",
                )
            } else if (state.historyItems.isEmpty()) {
                WalletInlineStatusCard(
                    title = "No transfer history yet",
                    supporting = "When you send or receive money, the receipt list will appear here and remain available offline.",
                )
            } else {
                state.historyItems.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction(WalletAction.OpenReceiptDetail(item.paymentId)) },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                WalletDirectionBadge(
                                    text = if (item.direction == WalletTransferDirection.Sent) "Sent" else "Received",
                                    accentColor = if (item.direction == WalletTransferDirection.Sent) WalletNegativeAccent else WalletPositiveAccent,
                                )
                                Text(
                                    text = "${item.currency} ${item.amount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (item.direction == WalletTransferDirection.Sent) WalletNegativeAccent else WalletPositiveAccent,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                                Text(
                                    text = item.counterpartyDisplayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = ImTextPrimary,
                                )
                            Text(
                                text = "${item.status} · ${item.paidAt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = WalletSecondaryText,
                            )
                            Text(
                                text = "Tap to open full receipt",
                                style = MaterialTheme.typography.labelSmall,
                                color = WalletMutedText,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletReceiptPanel(state: WalletUiState, onAction: (WalletAction) -> Unit) {
    val receipt = state.selectedReceipt
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = WalletPanelColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Transfer receipt",
                style = MaterialTheme.typography.titleLarge,
                color = ImTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            if (state.isReceiptLoading && receipt == null) {
                WalletInlineStatusCard(
                    title = "Loading receipt details",
                    supporting = "Fetching the latest receipt details or falling back to your cached copy.",
                )
            } else if (receipt != null) {
                WalletDirectionBadge(
                    text = if (receipt.direction == WalletTransferDirection.Sent) "Sent receipt" else "Received receipt",
                    accentColor = if (receipt.direction == WalletTransferDirection.Sent) WalletNegativeAccent else WalletPositiveAccent,
                )
                ReceiptLine("Receipt ID", receipt.paymentId)
                ReceiptLine("Scan session", receipt.scanSessionId)
                ReceiptLine("Direction", receipt.direction.name)
                ReceiptLine("Payer account", receipt.payerAccountId)
                ReceiptLine("Payer", receipt.payerDisplayName)
                ReceiptLine("Payer balance after", "${receipt.currency} ${receipt.payerBalanceAfter}")
                ReceiptLine("Recipient account", receipt.recipientAccountId)
                ReceiptLine("Recipient", receipt.recipientDisplayName)
                ReceiptLine("Recipient balance after", "${receipt.currency} ${receipt.recipientBalanceAfter}")
                ReceiptLine("Amount", "${receipt.currency} ${receipt.amount}")
                ReceiptLine("Status", receipt.status)
                ReceiptLine("Processed at", receipt.paidAt)
                ReceiptLine("Note", receipt.note.ifBlank { "—" })
            } else {
                WalletInlineStatusCard(
                    title = "Receipt unavailable",
                    supporting = "This receipt is not available yet. Try refreshing when the network is back.",
                )
            }
            Button(
                onClick = { onAction(WalletAction.OpenTransactionHistory) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = state.openHistoryActionLabel)
            }
        }
    }
}

@Composable
private fun WalletSummaryHero(summary: WalletSummaryUi?, isLoading: Boolean) {
    val currencyText = summary?.currency ?: "CNY"
    val balanceText = summary?.balance ?: if (isLoading) "Loading" else "--"
    val identityText = when {
        summary != null -> "${summary.displayName} · ${summary.accountId}"
        isLoading -> "Cached wallet summary is loading..."
        else -> "Wallet summary unavailable offline."
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, WalletPrimaryAccent.copy(alpha = 0.24f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Ready to move money",
                style = MaterialTheme.typography.labelLarge,
                color = WalletMutedText,
            )
            Text(
                text = "$currencyText $balanceText",
                style = MaterialTheme.typography.headlineMedium,
                color = ImTextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = identityText,
                style = MaterialTheme.typography.bodyMedium,
                color = WalletSecondaryText,
            )
        }
    }
}

@Composable
private fun WalletInlineStatusCard(title: String, supporting: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = ImTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = WalletSecondaryText,
            )
        }
    }
}

@Composable
private fun WalletActionCard(
    title: String,
    subtitle: String,
    highlightColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = highlightColor.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, highlightColor.copy(alpha = 0.28f)),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = ImTextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = WalletSecondaryText,
            )
        }
    }
}

@Composable
private fun WalletSectionIntro(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = ImTextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = WalletSecondaryText,
        )
    }
}

@Composable
private fun WalletDirectionBadge(text: String, accentColor: Color = WalletPrimaryAccent) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accentColor.copy(alpha = 0.18f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = ImTextPrimary,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun WalletStatStrip(
    primaryLabel: String,
    primaryValue: String,
    secondaryLabel: String,
    secondaryValue: String,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WalletStatItem(
                label = primaryLabel,
                value = primaryValue,
                modifier = Modifier.weight(1f),
            )
            WalletStatItem(
                label = secondaryLabel,
                value = secondaryValue,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WalletStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = WalletMutedText,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = ImTextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ReceiptLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = WalletMutedText,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = ImTextPrimary,
        )
    }
}

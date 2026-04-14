package com.kzzz3.argus.lens.feature.scan

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun ScanScreen(
    state: ScanUiState,
    permissionRequestPending: Boolean,
    onAction: (ScanAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        onAction(ScanAction.CameraPermissionResult(granted))
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
            onAction(ScanAction.CameraPermissionResult(granted))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07131E),
                        Color(0xFF10304A),
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0x1A9AD0FF),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = state.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFD8EBFB),
                )
            }
        }

        state.statusMessage?.let { message ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (state.isStatusError) Color(0x33FF6F61) else Color(0x267AF5C9),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0x142D4258),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!state.cameraPermissionGranted) {
                    Text(
                        text = "Grant camera access to scan real merchant QR codes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFDCEBFA),
                    )
                    Button(
                        onClick = { onAction(ScanAction.RequestCameraPermission) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = state.permissionActionLabel)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        QrScannerPreview(
                            isActive = state.isScannerActive,
                            onPayloadDetected = { onAction(ScanAction.DetectedPayload(it)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                        )
                    }
                    Text(
                        text = state.scannerHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAC9E3),
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0x142D4258),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.manualPayload,
                    onValueChange = { onAction(ScanAction.UpdateManualPayload(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = state.manualPayloadLabel) },
                    placeholder = { Text(text = state.manualPayloadPlaceholder) },
                )
                Button(
                    onClick = { onAction(ScanAction.SubmitManualPayload) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = state.resolveActionLabel)
                }
            }
        }

        state.resolution?.let { resolution ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x1529FFB2),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = resolution.merchantDisplayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Merchant account: ${resolution.merchantAccountId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD8EBFB),
                    )
                    resolution.suggestedAmount?.let { amount ->
                        Text(
                            text = "Suggested amount: ${resolution.currency} $amount",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD8EBFB),
                        )
                    }
                    OutlinedTextField(
                        value = state.amountDraft,
                        onValueChange = { onAction(ScanAction.UpdateAmountDraft(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = resolution.amountEditable,
                        label = { Text(text = state.amountLabel) },
                    )
                    OutlinedTextField(
                        value = state.noteDraft,
                        onValueChange = { onAction(ScanAction.UpdateNoteDraft(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = state.noteLabel) },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { onAction(ScanAction.ConfirmPayment) },
                            enabled = state.canConfirm,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = state.confirmActionLabel)
                        }
                        Button(
                            onClick = { onAction(ScanAction.ResetForRescan) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = state.rescanActionLabel)
                        }
                    }
                }
            }
        }

        state.completedPayment?.let { receipt ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x1A29FFB2),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${receipt.merchantDisplayName} · ${receipt.currency} ${receipt.amount}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Status: ${receipt.status}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFDCEBFA),
                    )
                    Text(
                        text = "Paid at: ${receipt.paidAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAC9E3),
                    )
                    if (receipt.note.isNotBlank()) {
                        Text(
                            text = "Note: ${receipt.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFAAC9E3),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { onAction(ScanAction.OpenReceiptConversation) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = state.openConversationActionLabel)
                        }
                        Button(
                            onClick = { onAction(ScanAction.ResetForRescan) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = state.rescanActionLabel)
                        }
                    }
                }
            }
        }

        Button(
            onClick = { onAction(ScanAction.NavigateBack) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = state.backActionLabel)
        }
    }
}

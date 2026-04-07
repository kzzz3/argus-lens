package com.kzzz3.argus.lens.feature.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kzzz3.argus.lens.ui.theme.ArguslensTheme

@Composable
fun CallSessionScreen(
    state: CallSessionUiState,
    onAction: (CallSessionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF071018),
                        Color(0xFF103049),
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0x142BFFC8),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = state.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFD8EBFB)
                )
                Text(
                    text = state.modeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF7AF5C9),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = state.statusLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFAEC7DC)
                )
                Text(
                    text = state.durationLabel,
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { onAction(CallSessionAction.ToggleMute) }, modifier = Modifier.weight(1f)) {
                Text(text = state.muteActionLabel)
            }
            Button(onClick = { onAction(CallSessionAction.ToggleSpeaker) }, modifier = Modifier.weight(1f)) {
                Text(text = state.speakerActionLabel)
            }
        }

        if (state.isCameraActionVisible) {
            Button(
                onClick = { onAction(CallSessionAction.ToggleCamera) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = state.cameraActionLabel)
            }
        }

        Button(
            onClick = { onAction(CallSessionAction.EndCall) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = state.endCallActionLabel)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CallSessionScreenPreview() {
    ArguslensTheme {
        CallSessionScreen(
            state = CallSessionUiState(
                title = "Zhang San",
                subtitle = "Stage-1 local call shell",
                modeLabel = "Video call",
                statusLabel = "Connecting locally...",
                durationLabel = "00:00",
                muteActionLabel = "Mute",
                speakerActionLabel = "Speaker off",
                cameraActionLabel = "Camera off",
                endCallActionLabel = "End call",
                isCameraActionVisible = true,
            ),
            onAction = {}
        )
    }
}

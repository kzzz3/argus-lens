package com.kzzz3.argus.lens.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class HomeHudUiState(
    val deviceLabel: String,
    val syncStatus: String,
    val activeMode: String,
    val primaryHint: String,
)

@Composable
fun HomeHudScreen(
    state: HomeHudUiState,
    onPrimaryActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF03111F),
                        Color(0xFF071D33),
                        Color(0xFF0C2B47),
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ARGUS LENS",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF9AD0FF),
                    fontWeight = FontWeight.Bold
                )
                StatusChip(label = state.syncStatus)
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0x221DF2FF)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = state.deviceLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Current mode: ${state.activeMode}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFE2F3FF)
                    )
                    Text(
                        text = state.primaryHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB8D9F4)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x14FFFFFF)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Learning focus for module 01",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "1. MainActivity only hosts the app entry.\n2. App-level composable decides which screen to show.\n3. Screen reads a plain UI state model.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD6E8F8)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x1429FFB2)
            ) {
                androidx.compose.material3.Button(
                    onClick = onPrimaryActionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(text = "Open login + inbox flow")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0x2629FFB2)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF8DFFCC),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeHudScreenPreview() {
    ArguslensTheme {
        HomeHudScreen(
            state = HomeHudUiState(
                deviceLabel = "Android Glasses Simulator",
                syncStatus = "Stage 1 Baseline Ready",
                activeMode = "IM Foundation",
                primaryHint = "Current module: local inbox + chat shell"
            ),
            onPrimaryActionClick = {}
        )
    }
}

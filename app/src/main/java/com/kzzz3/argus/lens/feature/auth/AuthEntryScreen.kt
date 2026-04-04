package com.kzzz3.argus.lens.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kzzz3.argus.lens.ui.theme.ArguslensTheme

data class AuthEntryUiState(
    val title: String,
    val subtitle: String,
    val primaryActionLabel: String,
    val secondaryActionLabel: String,
)

@Composable
fun AuthEntryScreen(
    state: AuthEntryUiState,
    onPrimaryActionClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF10141D),
                        Color(0xFF151E2E),
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0x1FFFFFFF)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = state.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFE1EBF5)
                )
                Text(
                    text = "Learning goal: understand route switching before adding real login state.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFAAC0D5)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0x1429FFB2)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPrimaryActionClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = state.primaryActionLabel)
                }
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = state.secondaryActionLabel)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthEntryScreenPreview() {
    ArguslensTheme {
        AuthEntryScreen(
            state = AuthEntryUiState(
                title = "Stage 1 Login Entry",
                subtitle = "We start with a fake login shell before touching real networking.",
                primaryActionLabel = "Enter login module",
                secondaryActionLabel = "Back to HUD"
            ),
            onPrimaryActionClick = {},
            onBackClick = {}
        )
    }
}

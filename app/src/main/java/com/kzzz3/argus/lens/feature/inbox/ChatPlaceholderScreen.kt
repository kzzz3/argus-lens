package com.kzzz3.argus.lens.feature.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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

@Composable
fun ChatPlaceholderScreen(
    state: ChatPlaceholderUiState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1620),
                        Color(0xFF182635),
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0x1629FFB2)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = state.conversationTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = state.conversationSubtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFD8EBFB)
                )
                Text(
                    text = state.messagePreview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFAAC9E3)
                )
            }
        }

        Button(
            onClick = { onAction(ChatAction.NavigateBackToInbox) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = state.primaryActionLabel)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatPlaceholderScreenPreview() {
    ArguslensTheme {
        ChatPlaceholderScreen(
            state = ChatPlaceholderUiState(
                conversationTitle = "Zhang San",
                conversationSubtitle = "Stage-1 chat placeholder",
                messagePreview = "Next step: render a real message timeline here.",
                primaryActionLabel = "Back to inbox"
            ),
            onAction = {}
        )
    }
}

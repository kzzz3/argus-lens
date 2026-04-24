package com.kzzz3.argus.lens.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kzzz3.argus.lens.ui.theme.ImBackground
import com.kzzz3.argus.lens.ui.theme.ImGreen
import com.kzzz3.argus.lens.ui.theme.ImSurfaceElevated
import com.kzzz3.argus.lens.ui.theme.ImTextPrimary
import com.kzzz3.argus.lens.ui.theme.ImTextSecondary

@Composable
internal fun AppLaunchPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ImBackground),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = ImSurfaceElevated.copy(alpha = 0.96f),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "ARGUS",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ImTextPrimary,
                )
                Text(
                    text = "Loading your workspace...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ImTextSecondary,
                )
                Text(
                    text = "Restoring your last session and refreshing account state.",
                    style = MaterialTheme.typography.labelMedium,
                    color = ImGreen,
                )
            }
        }
    }
}

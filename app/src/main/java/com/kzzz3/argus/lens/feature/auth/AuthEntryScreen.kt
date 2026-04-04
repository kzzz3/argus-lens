package com.kzzz3.argus.lens.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.kzzz3.argus.lens.ui.theme.ArguslensTheme

@Composable
fun AuthEntryScreen(
    state: AuthEntryUiState,
    onModeChange: (AuthLoginMode) -> Unit,
    onAccountChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AuthModeButton(
                        label = "Password",
                        selected = state.selectedMode == AuthLoginMode.Password,
                        onClick = { onModeChange(AuthLoginMode.Password) },
                        modifier = Modifier.weight(1f)
                    )
                    AuthModeButton(
                        label = "Code",
                        selected = state.selectedMode == AuthLoginMode.VerificationCode,
                        onClick = { onModeChange(AuthLoginMode.VerificationCode) },
                        modifier = Modifier.weight(1f)
                    )
                }
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
                if (state.selectedMode == AuthLoginMode.Password) {
                    OutlinedTextField(
                        value = state.account,
                        onValueChange = onAccountChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Account") },
                        placeholder = { Text(text = "Enter username or email") },
                        isError = state.accountError != null,
                        singleLine = true,
                    )

                    if (state.accountError != null) {
                        Text(
                            text = state.accountError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Password") },
                        placeholder = { Text(text = "Enter password") },
                        isError = state.passwordError != null,
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )

                    if (state.passwordError != null) {
                        Text(
                            text = state.passwordError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (state.submitResult != null) {
                        Text(
                            text = state.submitResult,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB8D9F4)
                        )
                    }

                    Button(
                        onClick = onPrimaryActionClick,
                        enabled = state.isPrimaryActionEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7AF5C9),
                            contentColor = Color(0xFF062118),
                            disabledContainerColor = Color(0xCC4E6A61),
                            disabledContentColor = Color(0xFFF2FFFA)
                        )
                    ) {
                        Text(text = state.primaryActionLabel)
                    }
                } else {
                    Text(
                        text = "Verification code login is reserved for the next step. For now we only build account/password login.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE1EBF5)
                    )
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFF89A7BD)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            disabledContentColor = Color(0xFFE3EEF8)
                        )
                    ) {
                        Text(text = "Verification code module coming soon")
                    }
                }

                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFF9AD0FF)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF4FAFF)
                    )
                ) {
                    Text(text = state.secondaryActionLabel)
                }
            }
        }
    }
}

@Composable
private fun AuthModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF9AD0FF),
                contentColor = Color(0xFF062038)
            )
        ) {
            Text(text = label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            border = BorderStroke(1.dp, Color(0xFF7EA8C7)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFEAF6FF)
            )
        ) {
            Text(text = label)
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
                selectedMode = AuthLoginMode.Password,
                account = "",
                password = "",
                accountError = "Account is required",
                passwordError = "Password must be at least 6 characters",
                submitResult = "",
                isPrimaryActionEnabled = false,
                primaryActionLabel = "Sign in with password",
                secondaryActionLabel = "Back to HUD"
            ),
            onModeChange = {},
            onAccountChange = {},
            onPasswordChange = {},
            onPrimaryActionClick = {},
            onBackClick = {}
        )
    }
}

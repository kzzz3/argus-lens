package com.kzzz3.argus.lens.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kzzz3.argus.lens.ui.theme.ArguslensTheme
import com.kzzz3.argus.lens.ui.theme.ImBlue
import com.kzzz3.argus.lens.ui.theme.ImGreen
import com.kzzz3.argus.lens.ui.theme.ImSurfaceElevated
import com.kzzz3.argus.lens.ui.theme.ImTextPrimary
import com.kzzz3.argus.lens.ui.theme.ImTextSecondary

@Composable
fun AuthEntryScreen(
    state: AuthEntryUiState,
    onAction: (AuthEntryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(shape = RoundedCornerShape(18.dp), color = ImSurfaceElevated.copy(alpha = 0.96f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = ImTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = state.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ImTextSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AuthModeButton(
                        label = "Password",
                        selected = state.selectedMode == AuthLoginMode.Password,
                        onClick = { onAction(AuthEntryAction.ChangeMode(AuthLoginMode.Password)) },
                        modifier = Modifier.weight(1f),
                    )
                    AuthModeButton(
                        label = "Code",
                        selected = state.selectedMode == AuthLoginMode.VerificationCode,
                        onClick = { onAction(AuthEntryAction.ChangeMode(AuthLoginMode.VerificationCode)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ImSurfaceElevated.copy(alpha = 0.94f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.selectedMode == AuthLoginMode.Password) {
                    OutlinedTextField(
                        value = state.account,
                        onValueChange = { onAction(AuthEntryAction.ChangeAccount(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Account") },
                        placeholder = { Text(text = "Enter username or email") },
                        isError = state.accountError != null,
                        singleLine = true,
                    )

                    state.accountError?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { onAction(AuthEntryAction.ChangePassword(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Password") },
                        placeholder = { Text(text = "Enter password") },
                        isError = state.passwordError != null,
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )

                    state.passwordError?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    state.submitResult?.takeIf { it.isNotBlank() }?.let {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        ) {
                            Text(
                                text = it,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ImTextSecondary,
                            )
                        }
                    }

                    Button(
                        onClick = { onAction(AuthEntryAction.SubmitPasswordLogin) },
                        enabled = state.isPrimaryActionEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ImGreen,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(text = state.primaryActionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    OutlinedButton(
                        onClick = { onAction(AuthEntryAction.NavigateToRegister) },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, ImGreen.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ImTextPrimary),
                    ) {
                        Text(text = state.registerActionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "Verification code sign-in is reserved for a later pass.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ImTextPrimary,
                            )
                            Text(
                                text = "Password sign-in is the active path for this build.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ImTextSecondary,
                            )
                        }
                    }
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
                containerColor = ImBlue,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        ) {
            Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            border = BorderStroke(1.dp, ImBlue.copy(alpha = 0.35f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ImTextPrimary),
        ) {
            Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthEntryScreenPreview() {
    ArguslensTheme {
        AuthEntryScreen(
            state = AuthEntryUiState(
                title = "Welcome back",
                subtitle = "Sign in to restore your chats, contacts, wallet, and profile shell.",
                selectedMode = AuthLoginMode.Password,
                account = "",
                password = "",
                accountError = "Account is required",
                passwordError = "Password must be at least 6 characters",
                submitResult = "",
                isSubmitting = false,
                isPrimaryActionEnabled = false,
                primaryActionLabel = "Sign in with password",
                registerActionLabel = "Create new account",
            ),
            onAction = {},
        )
    }
}

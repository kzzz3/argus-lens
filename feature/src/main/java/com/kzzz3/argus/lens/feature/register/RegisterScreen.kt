package com.kzzz3.argus.lens.feature.register

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
fun RegisterScreen(
    state: RegisterUiState,
    onAction: (RegisterAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = ImSurfaceElevated.copy(alpha = 0.96f),
        ) {
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
                )
                Text(
                    text = state.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ImTextSecondary,
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                ) {
                    Text(
                        text = "Create your account once, then the app can reopen from cache and validate in the background like a real IM client.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ImTextSecondary,
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
                RegisterField(
                    value = state.displayName,
                    label = "Display name",
                    placeholder = "Enter display name",
                    error = state.displayNameError,
                    onValueChange = { onAction(RegisterAction.ChangeDisplayName(it)) },
                )
                RegisterField(
                    value = state.account,
                    label = "Account",
                    placeholder = "Enter username or email",
                    error = state.accountError,
                    onValueChange = { onAction(RegisterAction.ChangeAccount(it)) },
                )
                RegisterField(
                    value = state.password,
                    label = "Password",
                    placeholder = "Enter password",
                    error = state.passwordError,
                    onValueChange = { onAction(RegisterAction.ChangePassword(it)) },
                    password = true,
                )
                RegisterField(
                    value = state.confirmPassword,
                    label = "Confirm password",
                    placeholder = "Re-enter password",
                    error = state.confirmPasswordError,
                    onValueChange = { onAction(RegisterAction.ChangeConfirmPassword(it)) },
                    password = true,
                )

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
                    onClick = { onAction(RegisterAction.SubmitRegistration) },
                    enabled = state.isSubmitEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImGreen,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(text = state.primaryActionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = { onAction(RegisterAction.NavigateBackToLogin) },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, ImBlue.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ImTextPrimary),
                ) {
                    Text(text = state.secondaryActionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun RegisterField(
    value: String,
    label: String,
    placeholder: String,
    error: String?,
    onValueChange: (String) -> Unit,
    password: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = label) },
        placeholder = { Text(text = placeholder) },
        isError = error != null,
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
    )

    error?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    ArguslensTheme {
        RegisterScreen(
            state = RegisterUiState(
                title = "Create account",
                subtitle = "Set up your Argus identity before entering the IM shell.",
                displayName = "",
                account = "",
                password = "",
                confirmPassword = "",
                displayNameError = "Display name is required",
                accountError = "Account is required",
                passwordError = "Password must be at least 6 characters",
                confirmPasswordError = "Passwords do not match",
                submitResult = null,
                isSubmitting = false,
                isSubmitEnabled = false,
                primaryActionLabel = "Create account",
                secondaryActionLabel = "Back to login",
            ),
            onAction = {},
        )
    }
}

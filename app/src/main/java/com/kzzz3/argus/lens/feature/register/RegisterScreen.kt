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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kzzz3.argus.lens.ui.theme.ArguslensTheme

@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onAction: (RegisterAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0E1420),
                        Color(0xFF182437),
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
                    color = Color(0xFFDCEBFA)
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
                RegisterField(
                    value = state.displayName,
                    label = "Display name",
                    placeholder = "Enter display name",
                    error = state.displayNameError,
                    onValueChange = { onAction(RegisterAction.ChangeDisplayName(it)) }
                )
                RegisterField(
                    value = state.account,
                    label = "Account",
                    placeholder = "Enter username or email",
                    error = state.accountError,
                    onValueChange = { onAction(RegisterAction.ChangeAccount(it)) }
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

                if (state.submitResult != null) {
                    Text(
                        text = state.submitResult,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB8D9F4)
                    )
                }

                Button(
                    onClick = { onAction(RegisterAction.SubmitRegistration) },
                    enabled = state.isSubmitEnabled,
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

                OutlinedButton(
                    onClick = { onAction(RegisterAction.NavigateBackToLogin) },
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

    if (error != null) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
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
                subtitle = "We build a stage-1 registration shell before connecting to a real backend.",
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
            onAction = {}
        )
    }
}

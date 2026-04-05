package com.kzzz3.argus.lens.feature.auth

enum class AuthLoginMode {
    Password,
    VerificationCode,
}

data class AuthEntryUiState(
    val title: String,
    val subtitle: String,
    val selectedMode: AuthLoginMode,
    val account: String,
    val password: String,
    val accountError: String?,
    val passwordError: String?,
    val submitResult: String?,
    val isSubmitting: Boolean,
    val isPrimaryActionEnabled: Boolean,
    val primaryActionLabel: String,
    val registerActionLabel: String,
    val secondaryActionLabel: String,
)

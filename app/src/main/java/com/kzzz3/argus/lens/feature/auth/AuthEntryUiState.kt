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
    val primaryActionLabel: String,
    val secondaryActionLabel: String,
)

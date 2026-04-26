package com.kzzz3.argus.lens.feature.register

data class RegisterUiState(
    val title: String,
    val subtitle: String,
    val displayName: String,
    val account: String,
    val password: String,
    val confirmPassword: String,
    val displayNameError: String?,
    val accountError: String?,
    val passwordError: String?,
    val confirmPasswordError: String?,
    val submitResult: String?,
    val isSubmitting: Boolean,
    val isSubmitEnabled: Boolean,
    val primaryActionLabel: String,
    val secondaryActionLabel: String,
)

package com.kzzz3.argus.lens.feature.auth

private const val MinAccountLength = 4
private const val MinPasswordLength = 6

fun createAuthEntryUiState(
    selectedMode: AuthLoginMode,
    account: String,
    password: String,
    submitResult: String?,
): AuthEntryUiState {
    val trimmedAccount = account.trim()
    val accountError = when {
        selectedMode != AuthLoginMode.Password -> null
        trimmedAccount.isEmpty() -> "Account is required"
        trimmedAccount.length < MinAccountLength -> "Account must be at least 4 characters"
        else -> null
    }
    val passwordError = when {
        selectedMode != AuthLoginMode.Password -> null
        password.isEmpty() -> "Password is required"
        password.length < MinPasswordLength -> "Password must be at least 6 characters"
        else -> null
    }

    return AuthEntryUiState(
        title = "Stage 1 Login Entry",
        subtitle = "We start with a fake login shell before touching real networking.",
        selectedMode = selectedMode,
        account = account,
        password = password,
        accountError = accountError,
        passwordError = passwordError,
        submitResult = submitResult,
        isPrimaryActionEnabled =
            selectedMode == AuthLoginMode.Password && accountError == null && passwordError == null,
        primaryActionLabel = "Sign in with password",
        secondaryActionLabel = "Back to HUD"
    )
}

fun buildDemoPasswordSignInResult(account: String): String {
    val trimmedAccount = account.trim()
    return "Demo sign-in passed for $trimmedAccount. Real network login comes next."
}

package com.kzzz3.argus.lens.feature.auth

private const val MinAccountLength = 4
private const val MinPasswordLength = 6

fun createAuthEntryUiState(
    formState: AuthFormState,
): AuthEntryUiState {
    val trimmedAccount = formState.account.trim()
    val accountError = when {
        formState.mode != AuthLoginMode.Password -> null
        trimmedAccount.isEmpty() -> "Account is required"
        trimmedAccount.length < MinAccountLength -> "Account must be at least 4 characters"
        else -> null
    }
    val passwordError = when {
        formState.mode != AuthLoginMode.Password -> null
        formState.password.isEmpty() -> "Password is required"
        formState.password.length < MinPasswordLength -> "Password must be at least 6 characters"
        else -> null
    }

    return AuthEntryUiState(
        title = "Stage 1 Login Entry",
        subtitle = "We start with a fake login shell before touching real networking.",
        selectedMode = formState.mode,
        account = formState.account,
        password = formState.password,
        accountError = accountError,
        passwordError = passwordError,
        submitResult = formState.submitResult,
        isPrimaryActionEnabled =
            formState.mode == AuthLoginMode.Password && accountError == null && passwordError == null,
        primaryActionLabel = "Sign in with password",
        secondaryActionLabel = "Back to HUD"
    )
}

fun buildDemoPasswordSignInResult(formState: AuthFormState): String {
    val trimmedAccount = formState.account.trim()
    return "Demo sign-in passed for $trimmedAccount. Real network login comes next."
}

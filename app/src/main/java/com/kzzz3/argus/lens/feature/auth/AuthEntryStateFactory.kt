package com.kzzz3.argus.lens.feature.auth

private const val MinAccountLength = 4
private const val MinPasswordLength = 6

fun isPasswordLoginSubmittable(formState: AuthFormState): Boolean {
    val trimmedAccount = formState.account.trim()
    val accountValid = trimmedAccount.length >= MinAccountLength
    val passwordValid = formState.password.length >= MinPasswordLength
    return formState.mode == AuthLoginMode.Password && accountValid && passwordValid
}

fun createAuthEntryUiState(
    formState: AuthFormState,
): AuthEntryUiState {
    val trimmedAccount = formState.account.trim()
    val rawAccountError = when {
        formState.mode != AuthLoginMode.Password -> null
        trimmedAccount.isEmpty() -> "Account is required"
        trimmedAccount.length < MinAccountLength -> "Account must be at least 4 characters"
        else -> null
    }
    val rawPasswordError = when {
        formState.mode != AuthLoginMode.Password -> null
        formState.password.isEmpty() -> "Password is required"
        formState.password.length < MinPasswordLength -> "Password must be at least 6 characters"
        else -> null
    }

    val shouldShowAccountError = formState.accountTouched || formState.submitAttempted
    val shouldShowPasswordError = formState.passwordTouched || formState.submitAttempted
    val accountError = rawAccountError?.takeIf { shouldShowAccountError }
    val passwordError = rawPasswordError?.takeIf { shouldShowPasswordError }

    return AuthEntryUiState(
        title = "Stage 1 Login Entry",
        subtitle = "We start with a fake login shell before touching real networking.",
        selectedMode = formState.mode,
        account = formState.account,
        password = formState.password,
        accountError = accountError,
        passwordError = passwordError,
        submitResult = formState.submitResult,
        isPrimaryActionEnabled = isPasswordLoginSubmittable(formState),
        primaryActionLabel = "Sign in with password",
        secondaryActionLabel = "Back to HUD"
    )
}

fun buildDemoPasswordSignInResult(formState: AuthFormState): String {
    val trimmedAccount = formState.account.trim()
    return "Demo sign-in passed for $trimmedAccount. Real network login comes next."
}

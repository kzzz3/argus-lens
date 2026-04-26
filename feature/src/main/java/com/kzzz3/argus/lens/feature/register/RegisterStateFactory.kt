package com.kzzz3.argus.lens.feature.register

private const val MinRegisterAccountLength = 4
private const val MinRegisterPasswordLength = 6

fun isRegisterFormSubmittable(formState: RegisterFormState): Boolean {
    return formState.displayName.trim().isNotEmpty() &&
        formState.account.trim().length >= MinRegisterAccountLength &&
        formState.password.length >= MinRegisterPasswordLength &&
        formState.password == formState.confirmPassword
}

fun createRegisterUiState(formState: RegisterFormState): RegisterUiState {
    val trimmedDisplayName = formState.displayName.trim()
    val trimmedAccount = formState.account.trim()

    val rawDisplayNameError = if (trimmedDisplayName.isEmpty()) "Display name is required" else null
    val rawAccountError = when {
        trimmedAccount.isEmpty() -> "Account is required"
        trimmedAccount.length < MinRegisterAccountLength -> "Account must be at least 4 characters"
        else -> null
    }
    val rawPasswordError = when {
        formState.password.isEmpty() -> "Password is required"
        formState.password.length < MinRegisterPasswordLength -> "Password must be at least 6 characters"
        else -> null
    }
    val rawConfirmPasswordError = when {
        formState.confirmPassword.isEmpty() -> "Confirm password is required"
        formState.confirmPassword != formState.password -> "Passwords do not match"
        else -> null
    }

    val showDisplayNameError = formState.displayNameTouched || formState.submitAttempted
    val showAccountError = formState.accountTouched || formState.submitAttempted
    val showPasswordError = formState.passwordTouched || formState.submitAttempted
    val showConfirmError = formState.confirmPasswordTouched || formState.submitAttempted

    return RegisterUiState(
        title = "Create account",
        subtitle = "Create your account to sign in, restore your session, and continue into Argus.",
        displayName = formState.displayName,
        account = formState.account,
        password = formState.password,
        confirmPassword = formState.confirmPassword,
        displayNameError = rawDisplayNameError?.takeIf { showDisplayNameError },
        accountError = rawAccountError?.takeIf { showAccountError },
        passwordError = rawPasswordError?.takeIf { showPasswordError },
        confirmPasswordError = rawConfirmPasswordError?.takeIf { showConfirmError },
        submitResult = formState.submitResult,
        isSubmitting = formState.isSubmitting,
        isSubmitEnabled = isRegisterFormSubmittable(formState) && !formState.isSubmitting,
        primaryActionLabel = if (formState.isSubmitting) "Creating account..." else "Create account",
        secondaryActionLabel = "Back to login",
    )
}

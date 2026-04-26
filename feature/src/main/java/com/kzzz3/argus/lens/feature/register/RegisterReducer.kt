package com.kzzz3.argus.lens.feature.register

data class RegisterReducerResult(
    val formState: RegisterFormState,
    val effect: RegisterEffect? = null,
)

fun reduceRegisterFormState(
    currentState: RegisterFormState,
    action: RegisterAction,
): RegisterReducerResult {
    return when (action) {
        is RegisterAction.ChangeDisplayName -> RegisterReducerResult(
            formState = currentState.copy(
                displayName = action.value,
                displayNameTouched = true,
                isSubmitting = false,
                submitResult = null,
            )
        )

        is RegisterAction.ChangeAccount -> RegisterReducerResult(
            formState = currentState.copy(
                account = action.value,
                accountTouched = true,
                isSubmitting = false,
                submitResult = null,
            )
        )

        is RegisterAction.ChangePassword -> RegisterReducerResult(
            formState = currentState.copy(
                password = action.value,
                passwordTouched = true,
                isSubmitting = false,
                submitResult = null,
            )
        )

        is RegisterAction.ChangeConfirmPassword -> RegisterReducerResult(
            formState = currentState.copy(
                confirmPassword = action.value,
                confirmPasswordTouched = true,
                isSubmitting = false,
                submitResult = null,
            )
        )

        RegisterAction.SubmitRegistration -> RegisterReducerResult(
            formState = currentState.copy(
                displayNameTouched = true,
                accountTouched = true,
                passwordTouched = true,
                confirmPasswordTouched = true,
                submitAttempted = true,
                isSubmitting = isRegisterFormSubmittable(currentState),
                submitResult = null,
            ),
            effect = if (isRegisterFormSubmittable(currentState)) {
                RegisterEffect.SubmitRegistration(
                    displayName = currentState.displayName.trim(),
                    account = currentState.account.trim(),
                    password = currentState.password,
                )
            } else {
                null
            }
        )

        RegisterAction.NavigateBackToLogin -> RegisterReducerResult(
            formState = currentState.copy(isSubmitting = false, submitResult = null),
            effect = RegisterEffect.NavigateBackToLogin,
        )
    }
}

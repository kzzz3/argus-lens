package com.kzzz3.argus.lens.feature.auth

data class AuthReducerResult(
    val formState: AuthFormState,
    val effect: AuthEntryEffect? = null,
)

fun reduceAuthFormState(
    currentState: AuthFormState,
    action: AuthEntryAction,
): AuthReducerResult {
    return when (action) {
        is AuthEntryAction.ChangeMode -> AuthReducerResult(
            formState = currentState.copy(
                mode = action.mode,
                accountTouched = false,
                passwordTouched = false,
                submitAttempted = false,
                isSubmitting = false,
                submitResult = null,
            )
        )

        is AuthEntryAction.ChangeAccount -> AuthReducerResult(
            formState = currentState.copy(
                account = action.value,
                accountTouched = true,
                isSubmitting = false,
                submitResult = null,
            )
        )

        is AuthEntryAction.ChangePassword -> AuthReducerResult(
            formState = currentState.copy(
                password = action.value,
                passwordTouched = true,
                isSubmitting = false,
                submitResult = null,
            )
        )

        AuthEntryAction.SubmitPasswordLogin -> AuthReducerResult(
            formState = currentState.copy(
                accountTouched = true,
                passwordTouched = true,
                submitAttempted = true,
                isSubmitting = isPasswordLoginSubmittable(currentState),
                submitResult = null,
            ),
            effect = if (isPasswordLoginSubmittable(currentState)) {
                AuthEntryEffect.SubmitPasswordLogin(
                    account = currentState.account.trim(),
                    password = currentState.password,
                )
            } else {
                null
            }
        )

        AuthEntryAction.NavigateToRegister -> AuthReducerResult(
            formState = currentState.copy(isSubmitting = false, submitResult = null),
            effect = AuthEntryEffect.NavigateToRegister,
        )

        AuthEntryAction.NavigateBack -> AuthReducerResult(
            formState = currentState.copy(isSubmitting = false, submitResult = null),
            effect = AuthEntryEffect.NavigateBack,
        )
    }
}

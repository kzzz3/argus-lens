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
                submitResult = null,
            )
        )

        is AuthEntryAction.ChangeAccount -> AuthReducerResult(
            formState = currentState.copy(
                account = action.value,
                accountTouched = true,
                submitResult = null,
            )
        )

        is AuthEntryAction.ChangePassword -> AuthReducerResult(
            formState = currentState.copy(
                password = action.value,
                passwordTouched = true,
                submitResult = null,
            )
        )

        AuthEntryAction.SubmitPasswordLogin -> AuthReducerResult(
            formState = currentState.copy(
                accountTouched = true,
                passwordTouched = true,
                submitAttempted = true,
                submitResult = if (isPasswordLoginSubmittable(currentState)) {
                    buildDemoPasswordSignInResult(currentState)
                } else {
                    null
                },
            )
        )

        AuthEntryAction.NavigateBack -> AuthReducerResult(
            formState = currentState.copy(submitResult = null),
            effect = AuthEntryEffect.NavigateBack,
        )
    }
}

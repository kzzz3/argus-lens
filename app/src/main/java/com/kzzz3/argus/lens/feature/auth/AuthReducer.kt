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
                submitResult = null,
            )
        )

        is AuthEntryAction.ChangeAccount -> AuthReducerResult(
            formState = currentState.copy(
                account = action.value,
                submitResult = null,
            )
        )

        is AuthEntryAction.ChangePassword -> AuthReducerResult(
            formState = currentState.copy(
                password = action.value,
                submitResult = null,
            )
        )

        AuthEntryAction.SubmitPasswordLogin -> AuthReducerResult(
            formState = currentState.copy(
                submitResult = buildDemoPasswordSignInResult(currentState),
            )
        )

        AuthEntryAction.NavigateBack -> AuthReducerResult(
            formState = currentState.copy(submitResult = null),
            effect = AuthEntryEffect.NavigateBack,
        )
    }
}

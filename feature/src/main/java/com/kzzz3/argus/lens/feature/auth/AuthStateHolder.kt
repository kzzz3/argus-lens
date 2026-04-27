package com.kzzz3.argus.lens.feature.auth

import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.feature.register.RegisterAction
import com.kzzz3.argus.lens.feature.register.RegisterEffect
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.feature.register.RegisterReducerResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthState(
    val authFormState: AuthFormState = AuthFormState(),
    val registerFormState: RegisterFormState = RegisterFormState(),
)

data class AuthStateHolderCallbacks(
    val onNavigateToRegister: () -> Unit,
    val onNavigateBackToLogin: () -> Unit,
    val applySuccessfulAuthResult: suspend (AuthRepositoryResult.Success, Boolean) -> Unit,
)

class AuthStateHolder(
    initialState: AuthState = AuthState(),
    private val scope: CoroutineScope,
    private val reduceAuthAction: (AuthFormState, AuthEntryAction) -> AuthReducerResult,
    private val reduceRegisterAction: (RegisterFormState, RegisterAction) -> RegisterReducerResult,
    private val login: suspend (AuthFormState, String, String) -> AuthSubmissionResult<AuthFormState>,
    private val register: suspend (RegisterFormState, String, String, String) -> AuthSubmissionResult<RegisterFormState>,
) {
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<AuthState> = mutableState.asStateFlow()

    fun handleAuthAction(
        action: AuthEntryAction,
        callbacks: AuthStateHolderCallbacks,
    ) {
        val requestState = mutableState.value.authFormState
        val result = reduceAuthAction(requestState, action)
        replaceAuthFormState(result.formState)

        when (val effect = result.effect) {
            AuthEntryEffect.NavigateBack -> Unit
            AuthEntryEffect.NavigateToRegister -> callbacks.onNavigateToRegister()
            is AuthEntryEffect.SubmitPasswordLogin -> {
                scope.launch {
                    when (val authResult = login(
                        requestState,
                        effect.account,
                        effect.password,
                    )) {
                        is AuthSubmissionResult.Success -> {
                            replaceAuthFormState(authResult.formState)
                            callbacks.applySuccessfulAuthResult(
                                authResult.authResult,
                                true,
                            )
                        }
                        is AuthSubmissionResult.Failure -> {
                            replaceAuthFormState(authResult.formState)
                        }
                    }
                }
            }
            null -> Unit
        }
    }

    fun handleRegisterAction(
        action: RegisterAction,
        callbacks: AuthStateHolderCallbacks,
    ) {
        val requestState = mutableState.value.registerFormState
        val result = reduceRegisterAction(requestState, action)
        replaceRegisterFormState(result.formState)

        when (val effect = result.effect) {
            RegisterEffect.NavigateBackToLogin -> callbacks.onNavigateBackToLogin()
            is RegisterEffect.SubmitRegistration -> {
                scope.launch {
                    when (val authResult = register(
                        requestState,
                        effect.displayName,
                        effect.account,
                        effect.password,
                    )) {
                        is AuthSubmissionResult.Success -> {
                            replaceRegisterFormState(authResult.formState)
                            callbacks.applySuccessfulAuthResult(
                                authResult.authResult,
                                false,
                            )
                        }
                        is AuthSubmissionResult.Failure -> {
                            replaceRegisterFormState(authResult.formState)
                        }
                    }
                }
            }
            null -> Unit
        }
    }

    fun replaceAuthFormState(formState: AuthFormState) {
        mutableState.update { state -> state.copy(authFormState = formState) }
    }

    fun replaceRegisterFormState(formState: RegisterFormState) {
        mutableState.update { state -> state.copy(registerFormState = formState) }
    }

    fun replaceState(state: AuthState) {
        mutableState.value = state
    }
}

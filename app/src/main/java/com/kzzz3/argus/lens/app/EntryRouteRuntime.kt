package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.feature.auth.AuthEntryAction
import com.kzzz3.argus.lens.feature.auth.AuthEntryEffect
import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.auth.AuthReducerResult
import com.kzzz3.argus.lens.feature.auth.AuthSubmissionResult
import com.kzzz3.argus.lens.feature.register.RegisterAction
import com.kzzz3.argus.lens.feature.register.RegisterEffect
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.feature.register.RegisterReducerResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal data class EntryRouteRequest(
    val authFormState: AuthFormState,
    val registerFormState: RegisterFormState,
)

internal data class EntryRouteCallbacks(
    val onRouteChanged: (AppRoute) -> Unit,
    val onAuthFormStateChanged: (AuthFormState) -> Unit,
    val onRegisterFormStateChanged: (RegisterFormState) -> Unit,
    val applySuccessfulAuthResult: suspend (AuthRepositoryResult.Success, Boolean) -> Unit,
)

internal class EntryRouteRuntime(
    private val scope: CoroutineScope,
    private val reduceAuthAction: (AuthFormState, AuthEntryAction) -> AuthReducerResult,
    private val reduceRegisterAction: (RegisterFormState, RegisterAction) -> RegisterReducerResult,
    private val login: suspend (AuthFormState, String, String) -> AuthSubmissionResult<AuthFormState>,
    private val register: suspend (RegisterFormState, String, String, String) -> AuthSubmissionResult<RegisterFormState>,
) {
    fun handleAuthAction(
        action: AuthEntryAction,
        request: EntryRouteRequest,
        callbacks: EntryRouteCallbacks,
    ) {
        val result = reduceAuthAction(request.authFormState, action)
        callbacks.onAuthFormStateChanged(result.formState)

        when (val effect = result.effect) {
            AuthEntryEffect.NavigateBack -> Unit
            AuthEntryEffect.NavigateToRegister -> callbacks.onRouteChanged(AppRoute.RegisterEntry)
            is AuthEntryEffect.SubmitPasswordLogin -> {
                scope.launch {
                    when (val authResult = login(
                        request.authFormState,
                        effect.account,
                        effect.password,
                    )) {
                        is AuthSubmissionResult.Success -> {
                            callbacks.onAuthFormStateChanged(authResult.formState)
                            callbacks.applySuccessfulAuthResult(
                                authResult.authResult,
                                true,
                            )
                        }
                        is AuthSubmissionResult.Failure -> {
                            callbacks.onAuthFormStateChanged(authResult.formState)
                        }
                    }
                }
            }
            null -> Unit
        }
    }

    fun handleRegisterAction(
        action: RegisterAction,
        request: EntryRouteRequest,
        callbacks: EntryRouteCallbacks,
    ) {
        val result = reduceRegisterAction(request.registerFormState, action)
        callbacks.onRegisterFormStateChanged(result.formState)

        when (val effect = result.effect) {
            RegisterEffect.NavigateBackToLogin -> callbacks.onRouteChanged(AppRoute.AuthEntry)
            is RegisterEffect.SubmitRegistration -> {
                scope.launch {
                    when (val authResult = register(
                        request.registerFormState,
                        effect.displayName,
                        effect.account,
                        effect.password,
                    )) {
                        is AuthSubmissionResult.Success -> {
                            callbacks.onRegisterFormStateChanged(authResult.formState)
                            callbacks.applySuccessfulAuthResult(
                                authResult.authResult,
                                false,
                            )
                        }
                        is AuthSubmissionResult.Failure -> {
                            callbacks.onRegisterFormStateChanged(authResult.formState)
                        }
                    }
                }
            }
            null -> Unit
        }
    }
}

package com.kzzz3.argus.lens.feature.auth

import com.kzzz3.argus.lens.core.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.core.data.auth.AuthSession
import com.kzzz3.argus.lens.feature.register.RegisterAction
import com.kzzz3.argus.lens.feature.register.RegisterEffect
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.feature.register.RegisterReducerResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthStateHolderTest {
    @Test
    fun handleAuthAction_updatesStateBeforeDispatchingNavigationEffect() {
        val reducedState = AuthFormState(account = "next")
        val holder = authStateHolder(
            reduceAuthAction = { _, action ->
                assertEquals(AuthEntryAction.NavigateToRegister, action)
                AuthReducerResult(
                    formState = reducedState,
                    effect = AuthEntryEffect.NavigateToRegister,
                )
            },
        )
        val events = mutableListOf<String>()

        holder.handleAuthAction(
            action = AuthEntryAction.NavigateToRegister,
            callbacks = authCallbacks(
                onNavigateToRegister = { events += "register:${holder.state.value.authFormState.account}" },
            ),
        )

        assertEquals(reducedState, holder.state.value.authFormState)
        assertEquals(listOf("register:next"), events)
    }

    @Test
    fun handleAuthAction_successUsesRequestSnapshotThenAppliesAuthResult() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val requestState = AuthFormState(account = "alice", password = "secret123")
        val reducedState = requestState.copy(isSubmitting = true)
        val successState = requestState.copy(isSubmitting = false, submitResult = "welcome")
        val successResult = authSuccess("alice")
        var loginFormState: AuthFormState? = null
        var appliedResult: AuthRepositoryResult.Success? = null
        var keepSubmitMessage: Boolean? = null
        lateinit var holder: AuthStateHolder
        holder = authStateHolder(
            scope = scope,
            initialState = AuthState(authFormState = requestState),
            reduceAuthAction = { _, _ ->
                AuthReducerResult(
                    formState = reducedState,
                    effect = AuthEntryEffect.SubmitPasswordLogin(
                        account = "alice",
                        password = "secret123",
                    ),
                )
            },
            login = { formState, _, _ ->
                loginFormState = formState
                assertEquals(reducedState, holder.state.value.authFormState)
                AuthSubmissionResult.Success(
                    authResult = successResult,
                    formState = successState,
                )
            },
        )

        holder.handleAuthAction(
            action = AuthEntryAction.SubmitPasswordLogin,
            callbacks = authCallbacks(
                applySuccessfulAuthResult = { result, keep ->
                    appliedResult = result
                    keepSubmitMessage = keep
                },
            ),
        )

        assertEquals(requestState, loginFormState)
        assertEquals(successState, holder.state.value.authFormState)
        assertEquals(successResult, appliedResult)
        assertEquals(true, keepSubmitMessage)
        scope.cancel()
    }

    @Test
    fun handleAuthAction_failurePublishesFailureWithoutApplyingAuthResult() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val requestState = AuthFormState(account = "alice", password = "bad-secret")
        val reducedState = requestState.copy(isSubmitting = true)
        val failureState = requestState.copy(isSubmitting = false, submitResult = "denied")
        var appliedResult: AuthRepositoryResult.Success? = null
        val holder = authStateHolder(
            scope = scope,
            initialState = AuthState(authFormState = requestState),
            reduceAuthAction = { _, _ ->
                AuthReducerResult(
                    formState = reducedState,
                    effect = AuthEntryEffect.SubmitPasswordLogin(
                        account = "alice",
                        password = "bad-secret",
                    ),
                )
            },
            login = { _, _, _ -> AuthSubmissionResult.Failure(failureState) },
        )

        holder.handleAuthAction(
            action = AuthEntryAction.SubmitPasswordLogin,
            callbacks = authCallbacks(
                applySuccessfulAuthResult = { result, _ -> appliedResult = result },
            ),
        )

        assertEquals(failureState, holder.state.value.authFormState)
        assertNull(appliedResult)
        scope.cancel()
    }

    @Test
    fun handleRegisterAction_updatesStateBeforeDispatchingNavigationEffect() {
        val reducedState = RegisterFormState(account = "next")
        val holder = authStateHolder(
            reduceRegisterAction = { _, action ->
                assertEquals(RegisterAction.NavigateBackToLogin, action)
                RegisterReducerResult(
                    formState = reducedState,
                    effect = RegisterEffect.NavigateBackToLogin,
                )
            },
        )
        val events = mutableListOf<String>()

        holder.handleRegisterAction(
            action = RegisterAction.NavigateBackToLogin,
            callbacks = authCallbacks(
                onNavigateBackToLogin = { events += "login:${holder.state.value.registerFormState.account}" },
            ),
        )

        assertEquals(reducedState, holder.state.value.registerFormState)
        assertEquals(listOf("login:next"), events)
    }

    @Test
    fun handleRegisterAction_successUsesRequestSnapshotThenAppliesAuthResult() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val requestState = RegisterFormState(
            displayName = "Alice",
            account = "alice",
            password = "secret123",
            confirmPassword = "secret123",
        )
        val reducedState = requestState.copy(isSubmitting = true)
        val successState = requestState.copy(isSubmitting = false, submitResult = "created")
        val successResult = authSuccess("alice")
        var registerFormState: RegisterFormState? = null
        var appliedResult: AuthRepositoryResult.Success? = null
        var keepSubmitMessage: Boolean? = null
        lateinit var holder: AuthStateHolder
        holder = authStateHolder(
            scope = scope,
            initialState = AuthState(registerFormState = requestState),
            reduceRegisterAction = { _, _ ->
                RegisterReducerResult(
                    formState = reducedState,
                    effect = RegisterEffect.SubmitRegistration(
                        displayName = "Alice",
                        account = "alice",
                        password = "secret123",
                    ),
                )
            },
            register = { formState, _, _, _ ->
                registerFormState = formState
                assertEquals(reducedState, holder.state.value.registerFormState)
                AuthSubmissionResult.Success(
                    authResult = successResult,
                    formState = successState,
                )
            },
        )

        holder.handleRegisterAction(
            action = RegisterAction.SubmitRegistration,
            callbacks = authCallbacks(
                applySuccessfulAuthResult = { result, keep ->
                    appliedResult = result
                    keepSubmitMessage = keep
                },
            ),
        )

        assertEquals(requestState, registerFormState)
        assertEquals(successState, holder.state.value.registerFormState)
        assertEquals(successResult, appliedResult)
        assertEquals(false, keepSubmitMessage)
        scope.cancel()
    }

    private fun authStateHolder(
        scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
        initialState: AuthState = AuthState(),
        reduceAuthAction: (AuthFormState, AuthEntryAction) -> AuthReducerResult = { state, _ ->
            AuthReducerResult(state)
        },
        reduceRegisterAction: (RegisterFormState, RegisterAction) -> RegisterReducerResult = { state, _ ->
            RegisterReducerResult(state)
        },
        login: suspend (AuthFormState, String, String) -> AuthSubmissionResult<AuthFormState> = { state, _, _ ->
            AuthSubmissionResult.Failure(state)
        },
        register: suspend (RegisterFormState, String, String, String) -> AuthSubmissionResult<RegisterFormState> = { state, _, _, _ ->
            AuthSubmissionResult.Failure(state)
        },
    ): AuthStateHolder {
        return AuthStateHolder(
            initialState = initialState,
            scope = scope,
            reduceAuthAction = reduceAuthAction,
            reduceRegisterAction = reduceRegisterAction,
            login = login,
            register = register,
        )
    }

    private fun authCallbacks(
        onNavigateToRegister: () -> Unit = {},
        onNavigateBackToLogin: () -> Unit = {},
        applySuccessfulAuthResult: suspend (AuthRepositoryResult.Success, Boolean) -> Unit = { _, _ -> },
    ): AuthStateHolderCallbacks {
        return AuthStateHolderCallbacks(
            onNavigateToRegister = onNavigateToRegister,
            onNavigateBackToLogin = onNavigateBackToLogin,
            applySuccessfulAuthResult = applySuccessfulAuthResult,
        )
    }

    private fun authSuccess(accountId: String): AuthRepositoryResult.Success {
        return AuthRepositoryResult.Success(
            AuthSession(
                accountId = accountId,
                displayName = accountId,
                accessToken = "access-token",
                refreshToken = "refresh-token",
                message = "ok",
            )
        )
    }
}

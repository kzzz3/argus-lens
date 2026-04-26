package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.data.auth.AuthSession
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntryRouteRuntimeTest {
    @Test
    fun handleAuthAction_navigateToRegisterPublishesStateThenRoutes() {
        val reducedState = AuthFormState(account = "next")
        val runtime = entryRouteRuntime(
            reduceAuthAction = { _, _ ->
                AuthReducerResult(
                    formState = reducedState,
                    effect = AuthEntryEffect.NavigateToRegister,
                )
            },
        )
        var authState: AuthFormState? = null
        var routedTo: AppRoute? = null

        runtime.handleAuthAction(
            action = AuthEntryAction.NavigateToRegister,
            request = entryRouteRequest(authFormState = AuthFormState(account = "old")),
            callbacks = entryRouteCallbacks(
                onRouteChanged = { routedTo = it },
                onAuthFormStateChanged = { authState = it },
            ),
        )

        assertEquals(reducedState, authState)
        assertEquals(AppRoute.RegisterEntry, routedTo)
    }

    @Test
    fun handleAuthAction_successUsesRequestSnapshotThenAppliesAuthResult() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val requestState = AuthFormState(account = "alice", password = "secret")
        val reducedState = requestState.copy(isSubmitting = true)
        val successState = requestState.copy(isSubmitting = false, submitResult = "welcome")
        val successResult = authSuccess("alice")
        var loginFormState: AuthFormState? = null
        var appliedResult: AuthRepositoryResult.Success? = null
        var keepSubmitMessage: Boolean? = null
        val stateEvents = mutableListOf<AuthFormState>()
        val runtime = entryRouteRuntime(
            scope = scope,
            reduceAuthAction = { _, _ ->
                AuthReducerResult(
                    formState = reducedState,
                    effect = AuthEntryEffect.SubmitPasswordLogin(
                        account = "alice",
                        password = "secret",
                    ),
                )
            },
            login = { formState, _, _ ->
                loginFormState = formState
                AuthSubmissionResult.Success(
                    authResult = successResult,
                    formState = successState,
                )
            },
        )

        runtime.handleAuthAction(
            action = AuthEntryAction.SubmitPasswordLogin,
            request = entryRouteRequest(authFormState = requestState),
            callbacks = entryRouteCallbacks(
                onAuthFormStateChanged = stateEvents::add,
                applySuccessfulAuthResult = { result, keep ->
                    appliedResult = result
                    keepSubmitMessage = keep
                },
            ),
        )

        assertEquals(requestState, loginFormState)
        assertEquals(listOf(reducedState, successState), stateEvents)
        assertEquals(successResult, appliedResult)
        assertEquals(true, keepSubmitMessage)
        scope.cancel()
    }

    @Test
    fun handleAuthAction_failurePublishesFailureWithoutApplyingAuthResult() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val requestState = AuthFormState(account = "alice", password = "bad")
        val reducedState = requestState.copy(isSubmitting = true)
        val failureState = requestState.copy(isSubmitting = false, submitResult = "denied")
        var appliedResult: AuthRepositoryResult.Success? = null
        val stateEvents = mutableListOf<AuthFormState>()
        val runtime = entryRouteRuntime(
            scope = scope,
            reduceAuthAction = { _, _ ->
                AuthReducerResult(
                    formState = reducedState,
                    effect = AuthEntryEffect.SubmitPasswordLogin(
                        account = "alice",
                        password = "bad",
                    ),
                )
            },
            login = { _, _, _ -> AuthSubmissionResult.Failure(failureState) },
        )

        runtime.handleAuthAction(
            action = AuthEntryAction.SubmitPasswordLogin,
            request = entryRouteRequest(authFormState = requestState),
            callbacks = entryRouteCallbacks(
                onAuthFormStateChanged = stateEvents::add,
                applySuccessfulAuthResult = { result, _ -> appliedResult = result },
            ),
        )

        assertEquals(listOf(reducedState, failureState), stateEvents)
        assertNull(appliedResult)
        scope.cancel()
    }

    @Test
    fun handleRegisterAction_navigateBackRoutesToAuthEntry() {
        val reducedState = RegisterFormState(account = "next")
        val runtime = entryRouteRuntime(
            reduceRegisterAction = { _, _ ->
                RegisterReducerResult(
                    formState = reducedState,
                    effect = RegisterEffect.NavigateBackToLogin,
                )
            },
        )
        var registerState: RegisterFormState? = null
        var routedTo: AppRoute? = null

        runtime.handleRegisterAction(
            action = RegisterAction.NavigateBackToLogin,
            request = entryRouteRequest(registerFormState = RegisterFormState(account = "old")),
            callbacks = entryRouteCallbacks(
                onRouteChanged = { routedTo = it },
                onRegisterFormStateChanged = { registerState = it },
            ),
        )

        assertEquals(reducedState, registerState)
        assertEquals(AppRoute.AuthEntry, routedTo)
    }

    @Test
    fun handleRegisterAction_successUsesRequestSnapshotThenAppliesAuthResult() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val requestState = RegisterFormState(
            displayName = "Alice",
            account = "alice",
            password = "secret",
            confirmPassword = "secret",
        )
        val reducedState = requestState.copy(isSubmitting = true)
        val successState = requestState.copy(isSubmitting = false, submitResult = "created")
        val successResult = authSuccess("alice")
        var registerFormState: RegisterFormState? = null
        var appliedResult: AuthRepositoryResult.Success? = null
        var keepSubmitMessage: Boolean? = null
        val stateEvents = mutableListOf<RegisterFormState>()
        val runtime = entryRouteRuntime(
            scope = scope,
            reduceRegisterAction = { _, _ ->
                RegisterReducerResult(
                    formState = reducedState,
                    effect = RegisterEffect.SubmitRegistration(
                        displayName = "Alice",
                        account = "alice",
                        password = "secret",
                    ),
                )
            },
            register = { formState, _, _, _ ->
                registerFormState = formState
                AuthSubmissionResult.Success(
                    authResult = successResult,
                    formState = successState,
                )
            },
        )

        runtime.handleRegisterAction(
            action = RegisterAction.SubmitRegistration,
            request = entryRouteRequest(registerFormState = requestState),
            callbacks = entryRouteCallbacks(
                onRegisterFormStateChanged = stateEvents::add,
                applySuccessfulAuthResult = { result, keep ->
                    appliedResult = result
                    keepSubmitMessage = keep
                },
            ),
        )

        assertEquals(requestState, registerFormState)
        assertEquals(listOf(reducedState, successState), stateEvents)
        assertEquals(successResult, appliedResult)
        assertEquals(false, keepSubmitMessage)
        scope.cancel()
    }

    @Test
    fun handleRegisterAction_failurePublishesFailureWithoutApplyingAuthResult() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val requestState = RegisterFormState(
            displayName = "Alice",
            account = "alice",
            password = "bad",
            confirmPassword = "bad",
        )
        val reducedState = requestState.copy(isSubmitting = true)
        val failureState = requestState.copy(isSubmitting = false, submitResult = "denied")
        var appliedResult: AuthRepositoryResult.Success? = null
        val stateEvents = mutableListOf<RegisterFormState>()
        val runtime = entryRouteRuntime(
            scope = scope,
            reduceRegisterAction = { _, _ ->
                RegisterReducerResult(
                    formState = reducedState,
                    effect = RegisterEffect.SubmitRegistration(
                        displayName = "Alice",
                        account = "alice",
                        password = "bad",
                    ),
                )
            },
            register = { _, _, _, _ -> AuthSubmissionResult.Failure(failureState) },
        )

        runtime.handleRegisterAction(
            action = RegisterAction.SubmitRegistration,
            request = entryRouteRequest(registerFormState = requestState),
            callbacks = entryRouteCallbacks(
                onRegisterFormStateChanged = stateEvents::add,
                applySuccessfulAuthResult = { result, _ -> appliedResult = result },
            ),
        )

        assertEquals(listOf(reducedState, failureState), stateEvents)
        assertNull(appliedResult)
        scope.cancel()
    }

    private fun entryRouteRuntime(
        scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
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
    ): EntryRouteRuntime {
        return EntryRouteRuntime(
            scope = scope,
            reduceAuthAction = reduceAuthAction,
            reduceRegisterAction = reduceRegisterAction,
            login = login,
            register = register,
        )
    }

    private fun entryRouteRequest(
        authFormState: AuthFormState = AuthFormState(),
        registerFormState: RegisterFormState = RegisterFormState(),
    ): EntryRouteRequest {
        return EntryRouteRequest(
            authFormState = authFormState,
            registerFormState = registerFormState,
        )
    }

    private fun entryRouteCallbacks(
        onRouteChanged: (AppRoute) -> Unit = {},
        onAuthFormStateChanged: (AuthFormState) -> Unit = {},
        onRegisterFormStateChanged: (RegisterFormState) -> Unit = {},
        applySuccessfulAuthResult: suspend (AuthRepositoryResult.Success, Boolean) -> Unit = { _, _ -> },
    ): EntryRouteCallbacks {
        return EntryRouteCallbacks(
            onRouteChanged = onRouteChanged,
            onAuthFormStateChanged = onAuthFormStateChanged,
            onRegisterFormStateChanged = onRegisterFormStateChanged,
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

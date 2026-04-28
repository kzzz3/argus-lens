package com.kzzz3.argus.lens.feature.auth

import com.kzzz3.argus.lens.core.data.auth.AuthRepository
import com.kzzz3.argus.lens.core.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.feature.register.RegisterFormState

class AuthUseCases(
    private val authRepository: AuthRepository,
) {
    suspend fun login(
        formState: AuthFormState,
        account: String,
        password: String,
    ): AuthSubmissionResult<AuthFormState> {
        return when (val authResult = authRepository.login(account, password)) {
            is AuthRepositoryResult.Success -> AuthSubmissionResult.Success(
                authResult = authResult,
                formState = formState.copy(isSubmitting = false, submitResult = authResult.session.message),
            )
            is AuthRepositoryResult.Failure -> AuthSubmissionResult.Failure(
                formState = formState.copy(isSubmitting = false, submitResult = authResult.message),
            )
        }
    }

    suspend fun register(
        formState: RegisterFormState,
        displayName: String,
        account: String,
        password: String,
    ): AuthSubmissionResult<RegisterFormState> {
        return when (val authResult = authRepository.register(displayName, account, password)) {
            is AuthRepositoryResult.Success -> AuthSubmissionResult.Success(
                authResult = authResult,
                formState = formState.copy(isSubmitting = false, submitResult = authResult.session.message),
            )
            is AuthRepositoryResult.Failure -> AuthSubmissionResult.Failure(
                formState = formState.copy(isSubmitting = false, submitResult = authResult.message),
            )
        }
    }
}

sealed interface AuthSubmissionResult<out T> {
    data class Success<T>(
        val authResult: AuthRepositoryResult.Success,
        val formState: T,
    ) : AuthSubmissionResult<T>

    data class Failure<T>(
        val formState: T,
    ) : AuthSubmissionResult<T>
}

package com.kzzz3.argus.lens.feature.auth

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

data class AuthFormState(
    val mode: AuthLoginMode = AuthLoginMode.Password,
    val account: String = "",
    val password: String = "",
    val accountTouched: Boolean = false,
    val passwordTouched: Boolean = false,
    val submitAttempted: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitResult: String? = null,
) {
    companion object {
        private const val SAVER_VERSION = "auth-form-v2"

        val Saver: Saver<AuthFormState, Any> = listSaver(
            save = { state ->
                listOf(
                    SAVER_VERSION,
                    state.mode.name,
                    state.account,
                    state.password,
                    state.accountTouched,
                    state.passwordTouched,
                    state.submitAttempted,
                    state.isSubmitting,
                    state.submitResult.orEmpty(),
                )
            },
            restore = { values ->
                if (values.size != 9 || values[0] != SAVER_VERSION) {
                    null
                } else {
                    AuthFormState(
                        mode = AuthLoginMode.valueOf(values[1] as String),
                        account = values[2] as String,
                        password = values[3] as String,
                        accountTouched = values[4] as Boolean,
                        passwordTouched = values[5] as Boolean,
                        submitAttempted = values[6] as Boolean,
                        isSubmitting = values[7] as Boolean,
                        submitResult = (values[8] as String).ifEmpty { null },
                    )
                }
            }
        )
    }
}

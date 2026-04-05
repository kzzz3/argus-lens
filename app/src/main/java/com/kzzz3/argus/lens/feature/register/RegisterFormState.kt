package com.kzzz3.argus.lens.feature.register

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

data class RegisterFormState(
    val displayName: String = "",
    val account: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val displayNameTouched: Boolean = false,
    val accountTouched: Boolean = false,
    val passwordTouched: Boolean = false,
    val confirmPasswordTouched: Boolean = false,
    val submitAttempted: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitResult: String? = null,
) {
    companion object {
        private const val SAVER_VERSION = "register-form-v2"

        val Saver: Saver<RegisterFormState, Any> = listSaver(
            save = { state ->
                listOf(
                    SAVER_VERSION,
                    state.displayName,
                    state.account,
                    state.password,
                    state.confirmPassword,
                    state.displayNameTouched,
                    state.accountTouched,
                    state.passwordTouched,
                    state.confirmPasswordTouched,
                    state.submitAttempted,
                    state.isSubmitting,
                    state.submitResult.orEmpty(),
                )
            },
            restore = { values ->
                if (values.size != 12 || values[0] != SAVER_VERSION) {
                    null
                } else {
                    RegisterFormState(
                        displayName = values[1] as String,
                        account = values[2] as String,
                        password = values[3] as String,
                        confirmPassword = values[4] as String,
                        displayNameTouched = values[5] as Boolean,
                        accountTouched = values[6] as Boolean,
                        passwordTouched = values[7] as Boolean,
                        confirmPasswordTouched = values[8] as Boolean,
                        submitAttempted = values[9] as Boolean,
                        isSubmitting = values[10] as Boolean,
                        submitResult = (values[11] as String).ifEmpty { null },
                    )
                }
            }
        )
    }
}

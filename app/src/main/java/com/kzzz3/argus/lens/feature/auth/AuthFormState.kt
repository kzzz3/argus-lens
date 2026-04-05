package com.kzzz3.argus.lens.feature.auth

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

data class AuthFormState(
    val mode: AuthLoginMode = AuthLoginMode.Password,
    val account: String = "",
    val password: String = "",
    val submitResult: String? = null,
) {
    companion object {
        val Saver: Saver<AuthFormState, Any> = listSaver(
            save = { state ->
                listOf(
                    state.mode.name,
                    state.account,
                    state.password,
                    state.submitResult.orEmpty(),
                )
            },
            restore = { values ->
                AuthFormState(
                    mode = AuthLoginMode.valueOf(values[0] as String),
                    account = values[1] as String,
                    password = values[2] as String,
                    submitResult = (values[3] as String).ifEmpty { null },
                )
            }
        )
    }
}

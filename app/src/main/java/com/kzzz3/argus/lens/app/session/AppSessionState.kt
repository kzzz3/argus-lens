package com.kzzz3.argus.lens.app.session

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

data class AppSessionState(
    val isAuthenticated: Boolean = false,
    val accountId: String = "",
    val displayName: String = "",
) {
    companion object {
        val Saver: Saver<AppSessionState, Any> = listSaver(
            save = { state ->
                listOf(
                    state.isAuthenticated,
                    state.accountId,
                    state.displayName,
                )
            },
            restore = { values ->
                AppSessionState(
                    isAuthenticated = values[0] as Boolean,
                    accountId = values[1] as String,
                    displayName = values[2] as String,
                )
            }
        )
    }
}

fun createPlaceholderSession(accountId: String): AppSessionState {
    val trimmedAccount = accountId.trim()
    val displayName = trimmedAccount.ifEmpty { "Argus User" }
    return AppSessionState(
        isAuthenticated = true,
        accountId = trimmedAccount,
        displayName = displayName,
    )
}

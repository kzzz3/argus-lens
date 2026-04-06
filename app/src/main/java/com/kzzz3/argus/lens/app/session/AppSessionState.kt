package com.kzzz3.argus.lens.app.session

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

data class AppSessionState(
    val isAuthenticated: Boolean = false,
    val accountId: String = "",
    val displayName: String = "",
    val accessToken: String = "",
) {
    companion object {
        val Saver: Saver<AppSessionState, Any> = listSaver(
            save = { state ->
                listOf(
                    state.isAuthenticated,
                    state.accountId,
                    state.displayName,
                    state.accessToken,
                )
            },
            restore = { values ->
                if (values.size != 4) {
                    null
                } else {
                    AppSessionState(
                        isAuthenticated = values[0] as Boolean,
                        accountId = values[1] as String,
                        displayName = values[2] as String,
                        accessToken = values[3] as String,
                    )
                }
            }
        )
    }
}

fun createAuthenticatedSession(
    accountId: String,
    displayName: String,
    accessToken: String,
): AppSessionState {
    val trimmedAccount = accountId.trim()
    val resolvedDisplayName = displayName.trim().ifEmpty { trimmedAccount.ifEmpty { "Argus User" } }
    return AppSessionState(
        isAuthenticated = true,
        accountId = trimmedAccount,
        displayName = resolvedDisplayName,
        accessToken = accessToken,
    )
}

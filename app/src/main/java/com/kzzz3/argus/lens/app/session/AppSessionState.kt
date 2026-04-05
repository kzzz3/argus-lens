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
        private const val SAVER_VERSION = "app-session-v2"

        val Saver: Saver<AppSessionState, Any> = listSaver(
            save = { state ->
                listOf(
                    SAVER_VERSION,
                    state.isAuthenticated,
                    state.accountId,
                    state.displayName,
                    state.accessToken,
                )
            },
            restore = { values ->
                if (values.size != 5 || values[0] != SAVER_VERSION) {
                    null
                } else {
                    AppSessionState(
                        isAuthenticated = values[1] as Boolean,
                        accountId = values[2] as String,
                        displayName = values[3] as String,
                        accessToken = values[4] as String,
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

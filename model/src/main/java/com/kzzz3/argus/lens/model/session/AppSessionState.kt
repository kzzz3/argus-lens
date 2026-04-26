package com.kzzz3.argus.lens.model.session

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppSessionState(
    val isAuthenticated: Boolean = false,
    val accountId: String = "",
    val displayName: String = "",
) : Parcelable

fun createAuthenticatedSession(
    accountId: String,
    displayName: String,
): AppSessionState {
    val trimmedAccount = accountId.trim()
    val resolvedDisplayName = displayName.trim().ifEmpty { trimmedAccount.ifEmpty { "Argus User" } }
    return AppSessionState(
        isAuthenticated = true,
        accountId = trimmedAccount,
        displayName = resolvedDisplayName,
    )
}

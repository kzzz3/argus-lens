package com.kzzz3.argus.lens.feature.auth

sealed interface AuthEntryAction {
    data class ChangeMode(val mode: AuthLoginMode) : AuthEntryAction
    data class ChangeAccount(val value: String) : AuthEntryAction
    data class ChangePassword(val value: String) : AuthEntryAction
    data object SubmitPasswordLogin : AuthEntryAction
    data object NavigateToRegister : AuthEntryAction
    data object NavigateBack : AuthEntryAction
}

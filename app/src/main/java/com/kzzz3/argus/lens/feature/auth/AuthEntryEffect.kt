package com.kzzz3.argus.lens.feature.auth

sealed interface AuthEntryEffect {
    data object NavigateBack : AuthEntryEffect
    data object NavigateToRegister : AuthEntryEffect
    data class SubmitPasswordLogin(
        val account: String,
        val password: String,
    ) : AuthEntryEffect
}

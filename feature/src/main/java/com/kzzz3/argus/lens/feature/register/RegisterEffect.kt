package com.kzzz3.argus.lens.feature.register

sealed interface RegisterEffect {
    data object NavigateBackToLogin : RegisterEffect
    data class SubmitRegistration(
        val displayName: String,
        val account: String,
        val password: String,
    ) : RegisterEffect
}

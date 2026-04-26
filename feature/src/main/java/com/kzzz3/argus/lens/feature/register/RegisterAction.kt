package com.kzzz3.argus.lens.feature.register

sealed interface RegisterAction {
    data class ChangeDisplayName(val value: String) : RegisterAction
    data class ChangeAccount(val value: String) : RegisterAction
    data class ChangePassword(val value: String) : RegisterAction
    data class ChangeConfirmPassword(val value: String) : RegisterAction
    data object SubmitRegistration : RegisterAction
    data object NavigateBackToLogin : RegisterAction
}

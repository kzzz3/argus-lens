package com.kzzz3.argus.lens.feature.auth

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AuthFormState(
    val mode: AuthLoginMode = AuthLoginMode.Password,
    val account: String = "",
    val password: String = "",
    val accountTouched: Boolean = false,
    val passwordTouched: Boolean = false,
    val submitAttempted: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitResult: String? = null,
) : Parcelable

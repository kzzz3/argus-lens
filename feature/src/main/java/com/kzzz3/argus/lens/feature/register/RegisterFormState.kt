package com.kzzz3.argus.lens.feature.register

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RegisterFormState(
    val displayName: String = "",
    val account: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val displayNameTouched: Boolean = false,
    val accountTouched: Boolean = false,
    val passwordTouched: Boolean = false,
    val confirmPasswordTouched: Boolean = false,
    val submitAttempted: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitResult: String? = null,
) : Parcelable

package com.kzzz3.argus.lens.feature.auth

sealed interface AuthEntryEffect {
    data object NavigateBack : AuthEntryEffect
}

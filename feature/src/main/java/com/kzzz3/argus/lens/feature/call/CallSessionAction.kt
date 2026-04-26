package com.kzzz3.argus.lens.feature.call

sealed interface CallSessionAction {
    data object ToggleMute : CallSessionAction
    data object ToggleSpeaker : CallSessionAction
    data object ToggleCamera : CallSessionAction
    data object EndCall : CallSessionAction
}

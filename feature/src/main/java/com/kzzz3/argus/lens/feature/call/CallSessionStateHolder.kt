package com.kzzz3.argus.lens.feature.call

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CallSessionStateHolder(
    initialState: CallSessionState = CallSessionState(),
) {
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<CallSessionState> = mutableState.asStateFlow()

    fun replaceState(state: CallSessionState) {
        mutableState.value = state
    }

    fun reset() {
        mutableState.value = CallSessionState()
    }
}

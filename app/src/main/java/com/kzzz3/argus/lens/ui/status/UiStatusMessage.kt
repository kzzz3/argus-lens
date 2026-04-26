package com.kzzz3.argus.lens.ui.status

data class UiStatusMessage(
    val text: String,
    val isError: Boolean,
) {
    companion object {
        fun success(text: String): UiStatusMessage = UiStatusMessage(text = text, isError = false)

        fun error(text: String): UiStatusMessage = UiStatusMessage(text = text, isError = true)
    }
}

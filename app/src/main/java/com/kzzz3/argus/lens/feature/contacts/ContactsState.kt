package com.kzzz3.argus.lens.feature.contacts

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

data class ContactsState(
    val draftConversationName: String = "",
) {
    companion object {
        val Saver: Saver<ContactsState, Any> = listSaver(
            save = { state -> listOf(state.draftConversationName) },
            restore = { values ->
                if (values.size != 1) {
                    null
                } else {
                    ContactsState(draftConversationName = values[0] as String)
                }
            }
        )
    }
}

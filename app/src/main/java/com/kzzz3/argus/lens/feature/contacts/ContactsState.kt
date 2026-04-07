package com.kzzz3.argus.lens.feature.contacts

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

data class ContactsState(
    val draftConversationName: String = "",
    val creationMode: ConversationCreationMode = ConversationCreationMode.Direct,
) {
    companion object {
        val Saver: Saver<ContactsState, Any> = listSaver(
            save = { state -> listOf(state.draftConversationName, state.creationMode.name) },
            restore = { values ->
                if (values.size != 2) {
                    null
                } else {
                    ContactsState(
                        draftConversationName = values[0] as String,
                        creationMode = ConversationCreationMode.valueOf(values[1] as String),
                    )
                }
            }
        )
    }
}

enum class ConversationCreationMode {
    Direct,
    Group,
}

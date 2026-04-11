package com.kzzz3.argus.lens.feature.contacts

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContactsState(
    val draftConversationName: String = "",
    val draftFriendAccountId: String = "",
    val creationMode: ConversationCreationMode = ConversationCreationMode.Direct,
) : Parcelable

enum class ConversationCreationMode {
    Direct,
    Group,
}

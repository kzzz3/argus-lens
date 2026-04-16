package com.kzzz3.argus.lens.feature.contacts

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContactsState(
    val draftFriendAccountId: String = "",
    val statusMessage: String? = null,
    val isStatusError: Boolean = false,
) : Parcelable

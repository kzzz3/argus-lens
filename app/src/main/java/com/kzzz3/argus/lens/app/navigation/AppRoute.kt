package com.kzzz3.argus.lens.app.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class AppRoute: Parcelable {
    AuthEntry,
    RegisterEntry,
    Inbox,
    Contacts,
    NewFriends,
    Wallet,
    Me,
    CallSession,
    Chat;
}

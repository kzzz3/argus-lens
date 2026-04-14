package com.kzzz3.argus.lens.app.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class AppRoute: Parcelable {
    Home,
    AuthEntry,
    RegisterEntry,
    Inbox,
    Contacts,
    Scan,
    CallSession,
    Chat;
}

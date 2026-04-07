package com.kzzz3.argus.lens.app.navigation

import androidx.compose.runtime.saveable.Saver

enum class AppRoute {
    Home,
    AuthEntry,
    RegisterEntry,
    Inbox,
    Contacts,
    CallSession,
    Chat;

    companion object {
        val Saver: Saver<AppRoute, Any> = Saver(
            save = { route -> route.name },
            restore = { value ->
                val raw = value as? String ?: return@Saver null
                entries.firstOrNull { it.name == raw }
            },
        )
    }
}

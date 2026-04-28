package com.kzzz3.argus.lens.model.realtime

import java.io.Closeable

interface ConversationRealtimeSubscription : Closeable

interface ConversationRealtimeClient {
    fun connect(
        accessToken: String,
        lastEventId: String? = null,
        onConnected: () -> Unit = {},
        onClosed: () -> Unit = {},
        onEvent: (ConversationRealtimeEvent) -> Unit,
        onError: (Throwable) -> Unit = {},
    ): ConversationRealtimeSubscription
}

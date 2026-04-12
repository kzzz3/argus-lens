package com.kzzz3.argus.lens.data.realtime

import java.io.Closeable

interface ConversationRealtimeSubscription : Closeable

interface ConversationRealtimeClient {
    fun connect(
        accessToken: String,
        onEvent: (ConversationRealtimeEvent) -> Unit,
        onError: (Throwable) -> Unit = {},
    ): ConversationRealtimeSubscription
}

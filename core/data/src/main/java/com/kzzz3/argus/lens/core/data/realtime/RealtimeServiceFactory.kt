package com.kzzz3.argus.lens.core.data.realtime

import com.kzzz3.argus.lens.core.network.realtime.createConversationRealtimeClient as createNetworkConversationRealtimeClient
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeClient

fun createConversationRealtimeClient(): ConversationRealtimeClient {
    return createNetworkConversationRealtimeClient()
}

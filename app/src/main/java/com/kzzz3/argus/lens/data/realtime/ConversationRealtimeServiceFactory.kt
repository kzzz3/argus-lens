package com.kzzz3.argus.lens.data.realtime

import com.kzzz3.argus.lens.data.network.createAppBaseUrl
import com.kzzz3.argus.lens.data.network.createAppGson
import com.kzzz3.argus.lens.data.network.createAppHttpClient

fun createConversationRealtimeClient(): ConversationRealtimeClient {
    val gson = createAppGson()
    val okHttpClient = createAppHttpClient()

    return SseConversationRealtimeClient(
        okHttpClient = okHttpClient,
        gson = gson,
        baseUrl = createAppBaseUrl(),
    )
}

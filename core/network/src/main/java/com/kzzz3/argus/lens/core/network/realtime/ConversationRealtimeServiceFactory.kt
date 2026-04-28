package com.kzzz3.argus.lens.core.network.realtime

import com.kzzz3.argus.lens.core.network.createAppBaseUrl
import com.kzzz3.argus.lens.core.network.createAppGson
import com.kzzz3.argus.lens.core.network.createAppHttpClient
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeClient
import java.util.concurrent.TimeUnit

fun createConversationRealtimeClient(): ConversationRealtimeClient {
    val gson = createAppGson()
    val okHttpClient = createAppHttpClient(enableVerboseHttpLogs = false)
        .newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    return SseConversationRealtimeClient(
        okHttpClient = okHttpClient,
        gson = gson,
        baseUrl = createAppBaseUrl(),
    )
}

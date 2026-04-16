package com.kzzz3.argus.lens.data.realtime

import com.kzzz3.argus.lens.data.network.createAppBaseUrl
import com.kzzz3.argus.lens.data.network.createAppGson
import com.kzzz3.argus.lens.data.network.createAppHttpClient
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

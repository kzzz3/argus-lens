package com.kzzz3.argus.lens.data.realtime

import com.google.gson.Gson
import okhttp3.OkHttpClient

fun createConversationRealtimeClient(): ConversationRealtimeClient {
    val gson = Gson()
    val okHttpClient = OkHttpClient.Builder()
        .build()

    return SseConversationRealtimeClient(
        okHttpClient = okHttpClient,
        gson = gson,
    )
}

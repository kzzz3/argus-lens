package com.kzzz3.argus.lens.data.realtime

import com.google.gson.Gson
import com.kzzz3.argus.lens.BuildConfig
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

class SseConversationRealtimeClient(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson = Gson(),
    private val baseUrl: HttpUrl = BuildConfig.AUTH_BASE_URL.toHttpUrl(),
) : ConversationRealtimeClient {

    private val eventSourceFactory: EventSource.Factory = EventSources.createFactory(okHttpClient)

    override fun connect(
        accessToken: String,
        onEvent: (ConversationRealtimeEvent) -> Unit,
        onError: (Throwable) -> Unit,
    ): ConversationRealtimeSubscription {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/conversations/events")
                    .build(),
            )
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val eventSource = eventSourceFactory.newEventSource(
            request = request,
            listener = object : EventSourceListener() {
                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String,
                ) {
                    if (data.isBlank()) return
                    runCatching { gson.fromJson(data, ConversationRealtimeEvent::class.java) }
                        .onSuccess(onEvent)
                        .onFailure(onError)
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?,
                ) {
                    if (t != null) {
                        onError(t)
                    } else if (response != null) {
                        onError(IllegalStateException("SSE connection failed with HTTP ${response.code}"))
                    }
                }
            },
        )

        return object : ConversationRealtimeSubscription {
            override fun close() {
                eventSource.cancel()
            }
        }
    }
}

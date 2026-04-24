package com.kzzz3.argus.lens.data.network

import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkClientFactoryTest {

    @Test
    fun createAppHttpClient_usesBasicLoggingWhenVerboseLogsAreEnabled() {
        val client = createAppHttpClient(enableVerboseHttpLogs = true)

        val loggingInterceptor = client.interceptors
            .filterIsInstance<HttpLoggingInterceptor>()
            .single()

        assertEquals(HttpLoggingInterceptor.Level.BASIC, loggingInterceptor.level)
    }

    @Test
    fun createAppHttpClient_skipsLoggingInterceptorWhenVerboseLogsAreDisabled() {
        val client = createAppHttpClient(enableVerboseHttpLogs = false)

        assertTrue(client.interceptors.none(Interceptor::isLoggingInterceptor))
    }
}

private fun Interceptor.isLoggingInterceptor(): Boolean = this is HttpLoggingInterceptor

package com.nuvio.app.features.player

import com.nuvio.app.features.addons.desktopHttpClient
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dispatcher
import okhttp3.Request
import okhttp3.Response

internal actual object AddonPlayerEventTransport {
    private val client = desktopHttpClient.newBuilder()
        // newBuilder shares the original dispatcher unless explicitly replaced.
        // A slow subtitle queue must not consume the event's short delivery budget.
        .dispatcher(Dispatcher().apply { maxRequests = 8; maxRequestsPerHost = 8 })
        .callTimeout(2, TimeUnit.SECONDS)
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .build()

    actual suspend fun send(url: String): Unit = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder().url(url)
            .header("User-Agent", "libmpv")
            .header("Cache-Control", "no-store")
            .header("Accept", "application/json")
            .build()
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(IOException("Addon player event delivery failed"))
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!continuation.isActive) return
                    // The protocol ignores the response body. Do not read or retain it.
                    if (response.isSuccessful) continuation.resume(Unit)
                    else continuation.resumeWithException(IOException("Addon player event was not accepted"))
                }
            }
        })
    }
}

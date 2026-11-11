package com.nuvio.app.features.player

import com.nuvio.app.features.addons.desktopHttpClient
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response

class AddonPlayerEventTransportQueueTest {
    @Test fun slowSubtitleDownloadsDoNotStarveConsentedPositionEvents() = runBlocking {
        val occupied = CountDownLatch(5)
        val release = CountDownLatch(1)
        val executor = Executors.newCachedThreadPool()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = executor
        server.createContext("/hold") { exchange ->
            occupied.countDown()
            try { release.await(8, TimeUnit.SECONDS); exchange.sendResponseHeaders(204, -1) }
            finally { exchange.close() }
        }
        server.createContext("/event") { exchange ->
            exchange.sendResponseHeaders(204, -1); exchange.close()
        }
        server.start()
        val base = "http://127.0.0.1:${server.address.port}"
        val calls = (1..5).map { desktopHttpClient.newCall(Request.Builder().url("$base/hold").build()) }
        try {
            calls.forEach { it.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) = Unit
                override fun onResponse(call: Call, response: Response) { response.close() }
            }) }
            assertTrue(occupied.await(3, TimeUnit.SECONDS), "Loopback download queue was not saturated")
            val before = System.nanoTime()
            AddonPlayerEventTransport.send("$base/event")
            assertTrue((System.nanoTime() - before) / 1_000_000 < 1_500,
                "Position event waited behind unrelated downloads")
        } finally {
            release.countDown(); calls.forEach(Call::cancel)
            server.stop(0); executor.shutdownNow()
        }
    }
}

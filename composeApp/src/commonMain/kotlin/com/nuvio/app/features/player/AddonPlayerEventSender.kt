package com.nuvio.app.features.player

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.concurrent.Volatile

/** One bounded worker per player. No retry, timer, disk state, log or new dependency. */
internal class AddonPlayerEventSender(
    private val profileIsCurrent: () -> Boolean,
    private val send: suspend (String) -> Unit,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val onDelivered: (String) -> Unit = {},
) {
    private data class Packet(val targets: List<String>, val events: List<AddonPlayerEvent>)
    private val pending = Channel<Packet>(1, BufferOverflow.DROP_OLDEST)
    @Volatile private var last: Packet? = null
    @Volatile private var closed = false
    private val worker = scope.launch {
        try {
            for (packet in pending) {
                if (!profileIsCurrent()) continue
                withTimeoutOrNull(2_000L) {
                    supervisorScope {
                        packet.targets.map { target ->
                            async {
                                var delivered = true
                                for (event in packet.events) {
                                    if (!profileIsCurrent()) { delivered = false; break }
                                    try { send(event.url(target)) }
                                    catch (e: CancellationException) { throw e }
                                    catch (_: Exception) { delivered = false /* Never log private addon URLs. */ }
                                }
                                if (delivered && packet === last && profileIsCurrent() && packet.events.last().action != "stop") onDelivered(target)
                            }
                        }.awaitAll()
                    }
                }
            }
        } finally { scope.cancel() }
    }

    fun offer(targets: List<String>, events: List<AddonPlayerEvent>) {
        if (closed || !profileIsCurrent() || events.isEmpty()) return
        val packet = Packet(targets.distinct().take(8), events.take(2))
        if (packet.targets.isEmpty()) return
        last = packet
        pending.trySend(packet)
    }

    fun close() {
        if (closed) return
        closed = true
        last?.takeIf { it.events.last().action != "stop" }?.let { packet ->
            pending.trySend(packet.copy(events = listOf(packet.events.last().copy(action = "stop"))))
        }
        pending.close()
    }
}

package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class AddonPlayerEventsTest {
    @Test fun consentRefreshIsOnceAndOnlyForSelectedConsentedSource() {
        val gate = AddonConsentSubtitleRefresh()
        val target = "https://example.invalid/c/synthetic/manifest.json"
        assertFalse(gate.consume("file-a",target,target))
        gate.grant("file-a",setOf(target))
        assertFalse(gate.consume("file-a",target,"https://other.invalid/manifest.json"))
        assertTrue(gate.consume("file-a",target,target))
        assertFalse(gate.consume("file-a",target,target))
    }
    @Test fun consentRefreshCannotReloadAnotherFileOrEmbeddedTrack() {
        val gate = AddonConsentSubtitleRefresh()
        val target = "https://example.invalid/manifest.json"
        gate.grant("file-a",setOf(target))
        assertFalse(gate.consume("file-a",null,target))
        assertFalse(gate.consume("file-b",target,target))
        assertFalse(gate.consume("file-a",target,target))
    }
    private fun AddonPlayerEventPolicy.tick(
        position: Long = 185_000, now: Long = 0, playing: Boolean = true,
        loading: Boolean = false, ready: Boolean = true, ended: Boolean = false,
        speed: Float = 1f, id: String? = "tt1234567:4:17", key: Any = "release-a",
        type: String = "series", duration: Long = 1_200_000,
    ) = update(key, type, id, position, duration, playing, loading, ended, ready, speed, now)

    @Test fun resumeUsesActualMilliseconds() {
        val p = AddonPlayerEventPolicy()
        assertTrue(p.tick(position = 0, duration = 0).isEmpty())
        assertTrue(p.tick(position = 0, ready = false).isEmpty())
        assertTrue(p.tick(loading = true).isEmpty())
        assertEquals(AddonPlayerEvent("series", "tt1234567:4:17", "start", 185_000, 1_200_000), p.tick().single())
    }
    @Test fun ticksDoNotPollAndPauseResumeSeekAreExplicit() {
        val p = AddonPlayerEventPolicy()
        p.tick()
        for (n in 1..90) assertTrue(p.tick(position = 185_000 + n * 1000L, now = n * 1000L).isEmpty())
        assertEquals("pause", p.tick(position = 275_000, now = 90_000, playing = false).single().action)
        assertTrue(p.tick(position = 275_000, now = 120_000, playing = false).isEmpty())
        assertEquals("pause", p.tick(position = 600_000, now = 121_000, playing = false).single().action)
        assertEquals("start", p.tick(position = 600_000, now = 122_000).single().action)
    }
    @Test fun bufferingStopsExtrapolation() {
        val p = AddonPlayerEventPolicy(); p.tick()
        assertEquals("pause", p.tick(loading = true).single().action)
        assertTrue(p.tick(loading = true, now = 20_000).isEmpty())
        assertEquals("start", p.tick(now = 21_000).single().action)
    }
    @Test fun fileOrEpisodeChangeStopsOldIdentityBeforeNewStart() {
        val p = AddonPlayerEventPolicy(); p.tick()
        val next = p.tick(id = "tt1234567:4:18", key = "release-b")
        assertEquals(listOf("stop", "start"), next.map { it.action })
        assertEquals("tt1234567:4:17", next.first().id)
        assertEquals("tt1234567:4:18", next.last().id)
        assertEquals(listOf("stop", "start"), p.tick(id = "tt1234567:4:18", key = "release-c").map { it.action })
    }
    @Test fun stopIsIdempotentAndBadRateDoesNotLieAboutPosition() {
        val p = AddonPlayerEventPolicy(); p.tick()
        assertEquals("stop", p.tick(speed = 2f).single().action)
        assertTrue(p.tick(speed = 2f).isEmpty())
        assertEquals("start", p.tick().single().action)
        assertEquals("stop", p.tick(ended = true).single().action)
        assertTrue(p.tick(ended = true).isEmpty())
        assertEquals(null, p.stop())
    }
    @Test fun invalidSnapshotClearsEarlierPositionRatherThanReusingIt() {
        val p = AddonPlayerEventPolicy(); p.tick()
        assertEquals("stop", p.tick(duration = 0).single().action)
        assertTrue(p.tick(duration = 0).isEmpty())
        assertEquals("start", p.tick().single().action)
    }
    @Test fun titlesAndSubtitleLanguagesAreNotSpecialCased() {
        for ((type, id) in listOf("movie" to "tt0123456", "series" to "tt7654321:2:3", "series" to "kitsu:42:1:2")) {
            val e = AddonPlayerEventPolicy().tick(type = type, id = id).single()
            assertEquals(type, e.type); assertEquals(id, e.id)
        }
        for (id in listOf(null, "", "https://example.invalid/private?key=x", "a/b", "bad\nvalue", "x".repeat(129))) {
            assertTrue(AddonPlayerEventPolicy().tick(id = id).isEmpty())
        }
        assertTrue(AddonPlayerEventPolicy().tick(position = -1).isEmpty())
        assertTrue(AddonPlayerEventPolicy().tick(position = 1_200_001).isEmpty())
    }
    @Test fun urlEncodesIdentityPreservesAddonConfigAndHasNoMediaFields() {
        val e = AddonPlayerEvent("series", "tt1234567:4:17", "start", 185_000, 1_200_000)
        assertEquals("https://example.invalid/c/synthetic/player/series/tt1234567%3A4%3A17/action=start&currentTime=185000&duration=1200000.json?setting=synthetic", e.url("https://example.invalid/c/synthetic/manifest.json?setting=synthetic"))
        assertFalse(e.url("https://example.invalid/manifest.json").contains("filename"))
    }
    @Test fun profileMismatchNeverDispatchesAndClosingIsSafe() = runBlocking {
        var calls = 0
        val sender = AddonPlayerEventSender({ false }, { calls++ })
        sender.offer(listOf("https://example.invalid/manifest.json"), AddonPlayerEventPolicy().tick())
        sender.close(); sender.close(); delay(30)
        assertEquals(0, calls)
    }
    @Test fun failureDoesNotPreventOtherAddonsAndCloseEmitsStop() = runBlocking {
        val stopped = CompletableDeferred<Unit>()
        val actions = Channel<String>(8)
        val started = CompletableDeferred<Unit>()
        val sender = AddonPlayerEventSender({ true }, { url ->
            if (url.startsWith("https://bad.invalid")) error("synthetic failure")
            actions.trySend(url)
            if (url.contains("action=start")) started.complete(Unit)
            if (url.contains("action=stop")) stopped.complete(Unit)
        })
        sender.offer(listOf("https://bad.invalid/manifest.json", "https://good.invalid/manifest.json"), AddonPlayerEventPolicy().tick())
        kotlinx.coroutines.withTimeout(3000) { started.await() }
        sender.close()
        kotlinx.coroutines.withTimeout(3000) { stopped.await() }
        assertTrue(actions.receive().contains("action=start"))
        assertTrue(actions.receive().contains("action=stop"))
        assertTrue(actions.tryReceive().isFailure)
    }
    @Test fun targetFanoutIsBoundedAndDoesNotRetryFailures() = runBlocking {
        val received = Channel<String>(32)
        val sender = AddonPlayerEventSender({ true }, { url -> received.trySend(url); error("synthetic") })
        sender.offer((1..30).map { "https://example$it.invalid/manifest.json" }, AddonPlayerEventPolicy().tick())
        kotlinx.coroutines.withTimeout(3000) { repeat(8) { received.receive() } }
        delay(50)
        assertTrue(received.tryReceive().isFailure)
        sender.close()
    }
    @Test fun seekBurstsKeepOnlyLatestPendingPacket() = runBlocking {
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val received = Channel<String>(8)
        val sender = AddonPlayerEventSender({ true }, { url ->
            if (!entered.isCompleted) { entered.complete(Unit); release.await() }
            received.trySend(url)
        })
        val initial = AddonPlayerEventPolicy().tick().single()
        val targets = listOf("https://example.invalid/manifest.json")
        sender.offer(targets,listOf(initial))
        kotlinx.coroutines.withTimeout(3000) { entered.await() }
        repeat(100) { sender.offer(targets,listOf(initial.copy(currentTime=300_000+it.toLong()))) }
        release.complete(Unit)
        kotlinx.coroutines.withTimeout(3000) {
            assertTrue(received.receive().contains("currentTime=185000"))
            assertTrue(received.receive().contains("currentTime=300099"))
        }
        assertTrue(received.tryReceive().isFailure)
        sender.close()
    }
    @Test fun queuedWorkIsDroppedAfterProfileChange() = runBlocking {
        var active = true
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val received = Channel<String>(8)
        val sender = AddonPlayerEventSender({ active }, { url ->
            entered.complete(Unit); release.await(); received.trySend(url)
        })
        val initial = AddonPlayerEventPolicy().tick().single()
        val targets = listOf("https://example.invalid/manifest.json")
        sender.offer(targets,listOf(initial))
        kotlinx.coroutines.withTimeout(3000) { entered.await() }
        sender.offer(targets,listOf(initial.copy(currentTime=600_000)))
        active=false; release.complete(Unit)
        kotlinx.coroutines.withTimeout(3000) { received.receive() }
        delay(50)
        assertTrue(received.tryReceive().isFailure)
        sender.close()
    }
    @Test fun deliveredEventAcknowledgesOnlySuccessfulCurrentTarget() = runBlocking {
        val acknowledged = Channel<String>(8)
        val sender = AddonPlayerEventSender({ true }, { url ->
            if (url.startsWith("https://bad.invalid")) error("synthetic refusal")
        }, onDelivered = { acknowledged.trySend(it) })
        sender.offer(listOf("https://bad.invalid/manifest.json","https://good.invalid/manifest.json"),AddonPlayerEventPolicy().tick())
        kotlinx.coroutines.withTimeout(3000) {
            assertEquals("https://good.invalid/manifest.json",acknowledged.receive())
        }
        sender.close(); delay(30)
        assertTrue(acknowledged.tryReceive().isFailure)
    }
}

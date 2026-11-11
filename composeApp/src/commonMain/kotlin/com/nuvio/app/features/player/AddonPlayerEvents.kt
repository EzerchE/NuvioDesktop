package com.nuvio.app.features.player

import com.nuvio.app.features.addons.buildAddonResourceUrl
import kotlin.math.abs

internal data class AddonPlayerEvent(
    val type: String, val id: String, val action: String,
    val currentTime: Long, val duration: Long,
) {
    fun url(manifestUrl: String): String = buildAddonResourceUrl(
        manifestUrl, "player", type, id,
        "action=$action&currentTime=$currentTime&duration=$duration",
    )
}

/** No language, provider, media URL or filename enters the event payload. */
internal class AddonPlayerEventPolicy {
    private var previous: AddonPlayerEvent? = null
    private var reportedAt = 0L
    private var previousKey: Any? = null
    private var previousSpeed = 1f

    fun update(
        key: Any, type: String, id: String?, positionMs: Long, durationMs: Long,
        playing: Boolean, loading: Boolean, ended: Boolean, ready: Boolean,
        speed: Float, nowMs: Long,
    ): List<AddonPlayerEvent> {
        val result = mutableListOf<AddonPlayerEvent>()
        if (key != previousKey || previous?.let { it.type != type || it.id != id } == true) {
            stop()?.let(result::add)
            previousKey = key
        }
        if (ended) {
            stop()?.let(result::add)
            return result
        }
        // The standard protocol has no playback-rate field. Do not advertise a
        // misleading extrapolated position during nonstandard-speed playback.
        if (speed != 1f) {
            stop()?.let(result::add)
            return result
        }
        if (!ready || id == null || !id.matches(Regex("[A-Za-z0-9][A-Za-z0-9:._-]{0,127}")) ||
            type !in listOf("movie", "series") || durationMs !in 1..86_400_000L ||
            positionMs !in 0 until durationMs || !speed.isFinite() || speed <= 0f
        ) {
            stop()?.let(result::add)
            return result
        }
        // A stalled video must not make the server extrapolate continued playback.
        val action = if (playing && !loading) "start" else "pause"
        val old = previous
        if (old == null && loading) return result
        val elapsed = (nowMs - reportedAt).coerceAtLeast(0)
        val expected = (old?.currentTime ?: positionMs) +
            if (old?.action == "start") (elapsed * previousSpeed).toLong() else 0L
        val seek = old != null && abs(positionMs - expected) > 2_500L
        if (old == null || action != old.action || seek || speed != previousSpeed) {
            val event = AddonPlayerEvent(type, id, action, positionMs, durationMs)
            result += event
            previous = event
            reportedAt = nowMs
            previousSpeed = speed
        }
        return result
    }

    fun stop(): AddonPlayerEvent? = previous?.copy(action = "stop").also { previous = null }
}

/** Refresh only the selected, consented addon, once its current event arrived. */
internal class AddonConsentSubtitleRefresh {
    private var playbackKey: Any? = null
    private var targets = emptySet<String>()
    fun grant(key: Any, consentedTargets: Set<String>) {
        playbackKey = key
        targets = consentedTargets
    }
    fun consume(key: Any, selectedManifest: String?, deliveredManifest: String): Boolean {
        if (key != playbackKey) { playbackKey = null; targets = emptySet(); return false }
        if (selectedManifest == null || selectedManifest != deliveredManifest || deliveredManifest !in targets) return false
        playbackKey = null
        targets = emptySet()
        return true
    }
}

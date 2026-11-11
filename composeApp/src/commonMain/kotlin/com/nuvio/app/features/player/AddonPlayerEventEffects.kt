package com.nuvio.app.features.player

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.channels.Channel
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.addons.supportsResource
import com.nuvio.app.features.profiles.ProfileRepository
import kotlin.time.TimeSource

@Composable
internal fun AddonPlayerEventEffects(
    profileId: Int, key: Any, type: String, id: String?, positionMs: Long,
    durationMs: Long, playing: Boolean, loading: Boolean, ended: Boolean,
    ready: Boolean, speed: Float,
    selectedManifest: String?, onRefreshSubtitle: () -> Unit,
) {
    val addons by AddonRepository.uiState.collectAsState()
    // Consent is intentionally session-only until upstream agrees on persisted permissions.
    var allowed by remember(profileId) { mutableStateOf<Set<String>?>(null) }
    val candidates = addons.addons.filter {
        it.isActive && id != null && it.manifest?.supportsResource("player", type, id) == true
    }.distinctBy { it.manifestUrl }.take(8)
    val targets = candidates.filter { it.manifestUrl in allowed.orEmpty() }.map { it.manifestUrl }
    val clock = remember(profileId) { TimeSource.Monotonic.markNow() }
    val policy = remember(profileId) { AddonPlayerEventPolicy() }
    val refreshGate = remember(profileId) { AddonConsentSubtitleRefresh() }
    val acknowledgements = remember(profileId) { Channel<String>(8) }
    val currentKey by rememberUpdatedState(key)
    val currentSelectedManifest by rememberUpdatedState(selectedManifest)
    val refreshSubtitle by rememberUpdatedState(onRefreshSubtitle)
    val sender = remember(profileId) {
        AddonPlayerEventSender(
            profileIsCurrent = { ProfileRepository.activeProfileId == profileId },
            // Desktop's native subtitle download uses libmpv. Keep the event
            // transport identity consistent without changing video HTTP headers.
            send = { url -> httpGetTextWithHeaders(url, mapOf("Cache-Control" to "no-store", "User-Agent" to "libmpv")) },
            onDelivered = { target -> acknowledgements.trySend(target) },
        )
    }
    if (allowed == null && ProfileRepository.activeProfileId == profileId && candidates.isNotEmpty() && ready && !loading && durationMs > 0L) {
        AlertDialog(
            onDismissRequest = { allowed = emptySet() },
            title = { Text("Share playback position with add-ons?") },
            text = { Text("Only these add-ons will receive the video ID, position and play/pause/stop state for this session: " +
                candidates.joinToString { it.displayTitle } + ". No video URL or filename is sent.") },
            confirmButton = { TextButton(onClick = {
                val consented = candidates.map { it.manifestUrl }.toSet()
                refreshGate.grant(key, consented)
                allowed = consented
            }) { Text("Allow for this session") } },
            dismissButton = { TextButton(onClick = { allowed = emptySet() }) { Text("Not now") } },
        )
    }
    LaunchedEffect(profileId, key, type, id, targets, positionMs / 1000L, durationMs, playing, loading, ended, ready, speed) {
        if (targets.isNotEmpty() && ProfileRepository.activeProfileId == profileId) {
            sender.offer(targets, policy.update(key, type, id, positionMs, durationMs, playing, loading,
                ended, ready, speed, clock.elapsedNow().inWholeMilliseconds))
        }
    }
    LaunchedEffect(sender) {
        for (target in acknowledgements) {
            if (ProfileRepository.activeProfileId == profileId && refreshGate.consume(currentKey, currentSelectedManifest, target)) refreshSubtitle()
        }
    }
    DisposableEffect(sender) { onDispose { sender.close(); acknowledgements.close() } }
}

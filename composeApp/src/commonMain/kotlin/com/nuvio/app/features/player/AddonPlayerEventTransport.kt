package com.nuvio.app.features.player

/** Dedicated cancellable, bounded transport; integration is Desktop only. */
internal expect object AddonPlayerEventTransport {
    suspend fun send(url: String)
}

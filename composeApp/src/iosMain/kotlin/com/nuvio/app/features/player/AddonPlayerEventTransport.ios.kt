package com.nuvio.app.features.player

internal actual object AddonPlayerEventTransport {
    actual suspend fun send(url: String): Unit =
        error("Addon player events are not enabled on this platform")
}

package com.nuvio.app.features.player

import androidx.compose.runtime.Composable

@Composable
internal actual fun AddonPositionConsentPrompt(message: String, onAllow: () -> Unit, onDecline: () -> Unit) =
    AddonPositionConsentFallback(message, onAllow, onDecline)

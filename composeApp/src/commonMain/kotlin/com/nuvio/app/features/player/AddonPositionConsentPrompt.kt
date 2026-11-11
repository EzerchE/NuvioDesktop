package com.nuvio.app.features.player

import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

@Composable
internal expect fun AddonPositionConsentPrompt(message: String, onAllow: () -> Unit, onDecline: () -> Unit)

@Composable
internal fun AddonPositionConsentFallback(message: String, onAllow: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text("Share playback position with add-ons?") },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onAllow) { Text("Allow for this session") } },
        dismissButton = { TextButton(onClick = onDecline) { Text("Not now") } },
    )
}

package com.nuvio.app.features.player

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState

/** A real owned window stays above the heavyweight libmpv/WebView player surfaces. */
@Composable
internal actual fun AddonPositionConsentPrompt(message: String, onAllow: () -> Unit, onDecline: () -> Unit) {
    DialogWindow(
        onCloseRequest = onDecline,
        title = "Add-on playback position",
        state = rememberDialogState(width = 560.dp, height = 280.dp),
        resizable = false,
    ) {
        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Share playback position with add-ons?", style = MaterialTheme.typography.titleLarge)
                    Text(message, modifier = Modifier.weight(1f))
                    Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDecline) { Text("Not now") }
                        TextButton(onClick = onAllow) { Text("Allow for this session") }
                    }
                }
            }
        }
    }
}

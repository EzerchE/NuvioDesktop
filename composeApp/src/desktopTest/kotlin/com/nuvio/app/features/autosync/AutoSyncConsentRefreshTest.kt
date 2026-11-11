package com.nuvio.app.features.autosync

import com.nuvio.app.features.player.PlayerEngineController
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class AutoSyncConsentRefreshTest {
    private fun withController(block: (AutoSyncMpvEngineController, MutableList<Pair<String, List<Any?>>>) -> Unit) {
        val calls = mutableListOf<Pair<String, List<Any?>>>()
        val base = Proxy.newProxyInstance(javaClass.classLoader,
            arrayOf(PlayerEngineController::class.java)) { _, method, args ->
            calls += method.name to args.orEmpty().toList()
            when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                java.lang.Integer.TYPE -> 0
                else -> null
            }
        } as PlayerEngineController
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val coordinator = AutoSyncMpvCoordinator(scope, "file:///synthetic.mkv", emptyMap(),
            { emptyMap() }, { "nld" }, object : AutoSyncMpvPlatform {
                override suspend fun writeSubtitleFile(fileName: String, stalePrefix: String, content: String): String? = null
                override fun showMessage(text: String) = Unit
            })
        try { block(AutoSyncMpvEngineController(base, coordinator) { _, _ -> error("No retiming expected") }, calls) }
        finally { coordinator.dispose(); scope.cancel() }
    }

    @Test fun refreshKeepsTheSelectedTrackInsteadOfSelectingAnAlternative() = withController { controller, calls ->
        controller.refreshSelectedSubtitle("https://example.invalid/selected.srt", 0)
        assertEquals(listOf("https://example.invalid/selected.srt"), calls.filter { it.first == "setSubtitleUri" }.map { it.second.single() })
    }

    @Test fun refreshDoesNotResetAnExplicitManualDelay() = withController { controller, calls ->
        controller.cancelForManualSubtitleDelay()
        controller.setSubtitleDelayMs(700)
        controller.refreshSelectedSubtitle("https://example.invalid/selected.srt", 700)
        assertEquals(listOf(700), calls.filter { it.first == "setSubtitleDelayMs" }.map { it.second.single() })
        assertEquals(1, calls.count { it.first == "setSubtitleUri" })
    }

    @Test fun refreshKeepsAPersistedOffsetWithoutRequiringAnotherManualChange() = withController { controller, calls ->
        controller.refreshSelectedSubtitle("https://example.invalid/selected.srt", -500)
        assertEquals(0, calls.count { it.first == "setSubtitleDelayMs" })
        assertEquals(1, calls.count { it.first == "setSubtitleUri" })
    }

    @Test fun explicitZeroDelayAlsoRemainsAUserChoice() = withController { controller, calls ->
        controller.cancelForManualSubtitleDelay()
        controller.refreshSelectedSubtitle("https://example.invalid/selected.srt", 0)
        assertEquals(0, calls.count { it.first == "setSubtitleDelayMs" })
        assertEquals(1, calls.count { it.first == "setSubtitleUri" })
    }
}

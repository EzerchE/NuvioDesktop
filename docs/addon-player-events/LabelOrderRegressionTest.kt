package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

/** Companion regression for #1060, intentionally separate from player events. */
class LabelOrderRegressionTest {
    @Test fun sameLanguageAlternativesKeepUpstreamOrderAndIdentity() = runBlocking {
        val request=SubtitleAddonRequest("https://example.invalid/subtitles.json","synthetic.addon","Synthetic Addon")
        val tracks=parseAddonSubtitles("""{"subtitles":[
            {"id":"track-a","url":"https://example.invalid/a.srt","lang":"eng","label":"Release A · WEB"},
            {"id":"track-b","url":"https://example.invalid/b.srt","lang":"eng","label":"Release B · BluRay"}
        ]}""",request)
        assertEquals(listOf("track-a","track-b"),tracks.map{it.id})
        assertEquals(listOf("Release A · WEB","Release B · BluRay"),tracks.map{it.display})
        assertEquals(listOf("Release A · WEB","Release B · BluRay"),tracks.map{it.label})
        assertEquals(listOf("en","en"),tracks.map{it.language})
        assertEquals(listOf("https://example.invalid/a.srt","https://example.invalid/b.srt"),tracks.map{it.url})
        assertEquals(listOf("Synthetic Addon","Synthetic Addon"),tracks.map{it.addonName})
    }
}

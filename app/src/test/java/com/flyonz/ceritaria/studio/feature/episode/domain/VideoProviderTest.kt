package com.flyonz.ceritaria.studio.feature.episode.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoProviderTest {
    @Test
    fun mapsKnownProviders() {
        assertEquals(VideoProvider.YouTube, videoProviderFrom("youtube"))
        assertEquals(VideoProvider.Facebook, videoProviderFrom("facebook"))
    }

    @Test
    fun preservesUnknownProviderValue() {
        val provider = videoProviderFrom("future-provider")
        assertTrue(provider is VideoProvider.Unknown)
        assertEquals("future-provider", (provider as VideoProvider.Unknown).rawValue)
    }
}

package com.flyonz.ceritaria.studio.feature.episode.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeVideoUrlValidatorTest {
    @Test
    fun acceptsProductionYouTubeShapes() {
        assertTrue(
            EpisodeVideoUrlValidator.isValid(
                "youtube",
                "https://youtu.be/dQw4w9WgXcQ",
            ),
        )
        assertTrue(
            EpisodeVideoUrlValidator.isValid(
                "youtube",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            ),
        )
        assertTrue(
            EpisodeVideoUrlValidator.isValid(
                "youtube",
                "https://youtube.com/shorts/dQw4w9WgXcQ",
            ),
        )
    }

    @Test
    fun rejectsNonHttpsAndLookalikeYouTubeHosts() {
        assertFalse(
            EpisodeVideoUrlValidator.isValid(
                "youtube",
                "http://youtu.be/dQw4w9WgXcQ",
            ),
        )
        assertFalse(
            EpisodeVideoUrlValidator.isValid(
                "youtube",
                "https://youtube.com.evil.example/watch?v=dQw4w9WgXcQ",
            ),
        )
    }

    @Test
    fun acceptsProductionFacebookShapes() {
        assertTrue(
            EpisodeVideoUrlValidator.isValid(
                "facebook",
                "https://www.facebook.com/reel/123456",
            ),
        )
        assertTrue(
            EpisodeVideoUrlValidator.isValid(
                "facebook",
                "https://facebook.com/creator/videos/123456",
            ),
        )
        assertTrue(
            EpisodeVideoUrlValidator.isValid(
                "facebook",
                "https://m.facebook.com/watch?v=123456",
            ),
        )
    }

    @Test
    fun rejectsUnsupportedFacebookHostOrPath() {
        assertFalse(
            EpisodeVideoUrlValidator.isValid(
                "facebook",
                "https://fb.com/reel/123456",
            ),
        )
        assertFalse(
            EpisodeVideoUrlValidator.isValid(
                "facebook",
                "https://facebook.com/creator/posts/123456",
            ),
        )
    }
}

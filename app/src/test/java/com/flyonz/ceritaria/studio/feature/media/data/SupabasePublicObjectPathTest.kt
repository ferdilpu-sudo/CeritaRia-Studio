package com.flyonz.ceritaria.studio.feature.media.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SupabasePublicObjectPathTest {
    @Test
    fun extractsObjectPathOnlyFromExpectedProjectAndBucket() {
        val url = "https://project.supabase.co/storage/v1/object/public/series-media/owner/file.webp"

        assertEquals(
            "owner/file.webp",
            publicObjectPathOrNull(
                publicUrl = url,
                expectedBucket = "series-media",
                expectedProjectUrl = "https://project.supabase.co",
            ),
        )
    }

    @Test
    fun refusesDifferentBucket() {
        val url = "https://project.supabase.co/storage/v1/object/public/episode-media/owner/file.webp"

        assertNull(
            publicObjectPathOrNull(
                publicUrl = url,
                expectedBucket = "series-media",
                expectedProjectUrl = "https://project.supabase.co",
            ),
        )
    }

    @Test
    fun refusesLookalikePathOnExternalHost() {
        val url = "https://evil.example/storage/v1/object/public/series-media/owner/file.webp"

        assertNull(
            publicObjectPathOrNull(
                publicUrl = url,
                expectedBucket = "series-media",
                expectedProjectUrl = "https://project.supabase.co",
            ),
        )
    }

    @Test
    fun refusesNonHttpsUrl() {
        val url = "http://project.supabase.co/storage/v1/object/public/series-media/owner/file.webp"

        assertNull(
            publicObjectPathOrNull(
                publicUrl = url,
                expectedBucket = "series-media",
                expectedProjectUrl = "https://project.supabase.co",
            ),
        )
    }
}

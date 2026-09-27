package com.flyonz.ceritaria.studio.feature.media.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SupabasePublicObjectPathTest {
    @Test
    fun extractsObjectPathOnlyFromExpectedPublicBucket() {
        val url = "https://project.supabase.co/storage/v1/object/public/series-media/owner/file.webp"

        assertEquals(
            "owner/file.webp",
            publicObjectPathOrNull(url, "series-media"),
        )
    }

    @Test
    fun refusesDifferentBucket() {
        val url = "https://project.supabase.co/storage/v1/object/public/episode-media/owner/file.webp"

        assertNull(publicObjectPathOrNull(url, "series-media"))
    }

    @Test
    fun refusesExternalUrl() {
        assertNull(
            publicObjectPathOrNull(
                "https://images.example.com/cover.webp",
                "series-media",
            ),
        )
    }
}

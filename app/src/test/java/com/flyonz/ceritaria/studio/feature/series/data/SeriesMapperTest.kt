package com.flyonz.ceritaria.studio.feature.series.data

import com.flyonz.ceritaria.studio.core.model.PublishStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesMapperTest {
    @Test
    fun mapsVerifiedProductionFields() {
        val dto = SeriesRowDto(
            id = "series-1",
            slug = "wajah-kedua",
            title = "Wajah Kedua",
            shortSynopsis = "Short",
            synopsis = "Long",
            genres = listOf("Drama"),
            coverUrl = "https://example.com/cover.webp",
            heroUrl = null,
            isFeatured = true,
            isPublished = true,
            publishedAt = "2026-09-01T10:00:00Z",
            seoTitle = "SEO",
            seoDescription = null,
            createdAt = "2026-08-01T10:00:00Z",
            updatedAt = "2026-09-01T10:00:00Z",
        )

        val series = dto.toDomain()

        assertEquals("wajah-kedua", series.slug)
        assertEquals(PublishStatus.PUBLISHED, series.publishStatus)
        assertEquals(listOf("Drama"), series.genres)
        assertTrue(series.isFeatured)
    }
}

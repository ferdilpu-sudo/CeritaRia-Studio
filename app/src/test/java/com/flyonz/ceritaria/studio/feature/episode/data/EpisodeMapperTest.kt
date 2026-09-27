package com.flyonz.ceritaria.studio.feature.episode.data

import com.flyonz.ceritaria.studio.core.model.PublishStatus
import com.flyonz.ceritaria.studio.feature.episode.domain.VideoProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeMapperTest {
    @Test
    fun mapsVerifiedProductionFields() {
        val dto = EpisodeRowDto(
            id = "episode-1",
            seriesId = "series-1",
            episodeNumber = 3,
            slug = "episode-3",
            title = "Episode 3",
            shortSynopsis = "Short",
            recap = "Recap",
            highlights = listOf("Moment"),
            videoProvider = "youtube",
            videoUrl = "https://youtu.be/example",
            thumbnailUrl = null,
            durationSeconds = 90,
            isPublished = true,
            publishedAt = "2026-09-01T10:00:00Z",
            createdAt = "2026-08-01T10:00:00Z",
            updatedAt = "2026-09-01T10:00:00Z",
            series = EpisodeSeriesDto(id = "series-1", title = "Wajah Kedua"),
        )

        val episode = dto.toDomain()

        assertEquals(3, episode.episodeNumber)
        assertEquals("Wajah Kedua", episode.seriesTitle)
        assertEquals(VideoProvider.YouTube, episode.videoProvider)
        assertEquals(PublishStatus.PUBLISHED, episode.publishStatus)
    }
}

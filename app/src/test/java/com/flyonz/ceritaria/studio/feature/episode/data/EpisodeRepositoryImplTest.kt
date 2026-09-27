package com.flyonz.ceritaria.studio.feature.episode.data

import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.feature.episode.domain.EditableVideoProvider
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeQuery
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeSaveCommand
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeRepositoryImplTest {
    @Test
    fun pageUsesLookaheadRowToDetermineHasMore() = runTest {
        val repository = EpisodeRepositoryImpl(
            FakeEpisodeDataSource(rows = listOf(row("1"), row("2"), row("3"))),
        )

        val page = success(repository.getEpisodes(EpisodeQuery(page = 1, pageSize = 2)))

        assertEquals(2, page.items.size)
        assertEquals(1, page.page)
        assertTrue(page.hasMore)
    }

    @Test
    fun publishedCreateAssignsPublishedAt() = runTest {
        val dataSource = FakeEpisodeDataSource()
        val repository = EpisodeRepositoryImpl(dataSource)

        success(repository.saveEpisode(command(id = null, isPublished = true)))

        assertNotNull(dataSource.createdPayload?.publishedAt)
    }

    @Test
    fun unpublishPreservesExistingPublishedAt() = runTest {
        val dataSource = FakeEpisodeDataSource()
        val repository = EpisodeRepositoryImpl(dataSource)
        val existing = Instant.parse("2026-09-01T00:00:00Z")

        success(
            repository.saveEpisode(
                command(id = "episode-1", isPublished = false, publishedAt = existing),
            ),
        )

        assertEquals(existing.toString(), dataSource.updatedPayload?.publishedAt)
        assertEquals(false, dataSource.updatedPayload?.isPublished)
    }

    @Test
    fun uniqueConflictMapsToConflictError() = runTest {
        val repository = EpisodeRepositoryImpl(
            FakeEpisodeDataSource(saveFailure = EpisodeConflictException()),
        )

        val result = repository.saveEpisode(command(id = null))

        assertEquals(AppError.Conflict, (result as AppResult.Failure).error)
    }

    @Test
    fun softDeleteDelegatesToDataSource() = runTest {
        val dataSource = FakeEpisodeDataSource()
        val repository = EpisodeRepositoryImpl(dataSource)

        success(repository.softDeleteEpisode("episode-1"))

        assertEquals("episode-1", dataSource.deletedId)
    }

    @Test
    fun configurationFailureIsMapped() = runTest {
        val repository = EpisodeRepositoryImpl(
            FakeEpisodeDataSource(readFailure = IllegalArgumentException("missing config")),
        )

        val result = repository.getEpisodes(EpisodeQuery())

        assertEquals(AppError.Configuration, (result as AppResult.Failure).error)
    }

    private class FakeEpisodeDataSource(
        private val rows: List<EpisodeRowDto> = emptyList(),
        private val readFailure: Throwable? = null,
        private val saveFailure: Throwable? = null,
    ) : EpisodeDataSource {
        var createdPayload: EpisodeWriteDto? = null
        var updatedPayload: EpisodeWriteDto? = null
        var deletedId: String? = null

        override suspend fun fetchEpisodes(query: EpisodeQuery): List<EpisodeRowDto> {
            readFailure?.let { throw it }
            return rows
        }

        override suspend fun fetchEpisodeById(id: String): EpisodeRowDto? =
            rows.firstOrNull { it.id == id }

        override suspend fun createEpisode(payload: EpisodeWriteDto): EpisodeRowDto {
            saveFailure?.let { throw it }
            createdPayload = payload
            return rowFromPayload(payload)
        }

        override suspend fun updateEpisode(payload: EpisodeWriteDto): EpisodeRowDto {
            saveFailure?.let { throw it }
            updatedPayload = payload
            return rowFromPayload(payload)
        }

        override suspend fun softDeleteEpisode(id: String) {
            deletedId = id
        }

        private fun rowFromPayload(payload: EpisodeWriteDto) = EpisodeRowDto(
            id = payload.id,
            seriesId = payload.seriesId,
            episodeNumber = payload.episodeNumber,
            slug = payload.slug,
            title = payload.title,
            shortSynopsis = payload.shortSynopsis,
            recap = payload.recap,
            highlights = payload.highlights,
            videoProvider = payload.videoProvider,
            videoUrl = payload.videoUrl,
            thumbnailUrl = payload.thumbnailUrl,
            durationSeconds = payload.durationSeconds,
            isPublished = payload.isPublished,
            publishedAt = payload.publishedAt,
            seoTitle = payload.seoTitle,
            seoDescription = payload.seoDescription,
            createdAt = "2026-09-01T00:00:00Z",
            updatedAt = "2026-09-01T00:00:00Z",
        )
    }

    private fun command(
        id: String?,
        isPublished: Boolean = false,
        publishedAt: Instant? = null,
    ) = EpisodeSaveCommand(
        id = id,
        seriesId = "series-1",
        episodeNumber = 1,
        slug = "episode-1",
        title = "Episode 1",
        shortSynopsis = null,
        recap = null,
        highlights = emptyList(),
        videoProvider = EditableVideoProvider.YOUTUBE,
        videoUrl = "https://youtu.be/example",
        thumbnailUrl = null,
        durationSeconds = null,
        isPublished = isPublished,
        existingPublishedAt = publishedAt,
        seoTitle = null,
        seoDescription = null,
    )

    private fun row(id: String) = EpisodeRowDto(
        id = id,
        seriesId = "series-1",
        episodeNumber = id.toInt(),
        slug = "episode-$id",
        title = "Episode $id",
        videoProvider = "youtube",
        videoUrl = "https://example.com/$id",
        isPublished = false,
        createdAt = "2026-09-01T00:00:00Z",
        updatedAt = "2026-09-01T00:00:00Z",
    )

    private fun <T> success(result: AppResult<T>): T =
        (result as AppResult.Success).value
}

package com.flyonz.ceritaria.studio.feature.episode.data

import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeQuery
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeRepositoryImplTest {
    @Test
    fun pageUsesLookaheadRowToDetermineHasMore() = runTest {
        val dataSource = FakeEpisodeDataSource(
            rows = listOf(row("1"), row("2"), row("3")),
        )
        val repository = EpisodeRepositoryImpl(dataSource)

        val page = when (val result = repository.getEpisodes(EpisodeQuery(page = 1, pageSize = 2))) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> error("Expected success, got ${result.error}")
        }

        assertEquals(2, page.items.size)
        assertEquals(1, page.page)
        assertTrue(page.hasMore)
    }

    @Test
    fun configurationFailureIsMapped() = runTest {
        val repository = EpisodeRepositoryImpl(
            FakeEpisodeDataSource(failure = IllegalArgumentException("missing config")),
        )

        val result = repository.getEpisodes(EpisodeQuery())

        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.Configuration, (result as AppResult.Failure).error)
    }

    private class FakeEpisodeDataSource(
        private val rows: List<EpisodeRowDto> = emptyList(),
        private val failure: Throwable? = null,
    ) : EpisodeDataSource {
        override suspend fun fetchEpisodes(query: EpisodeQuery): List<EpisodeRowDto> {
            failure?.let { throw it }
            return rows
        }

        override suspend fun fetchEpisodeById(id: String): EpisodeRowDto? =
            rows.firstOrNull { it.id == id }
    }

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
}

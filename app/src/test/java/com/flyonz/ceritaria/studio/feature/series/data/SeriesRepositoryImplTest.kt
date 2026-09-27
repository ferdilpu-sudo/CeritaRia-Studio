package com.flyonz.ceritaria.studio.feature.series.data

import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesQuery
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesSaveCommand
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesRepositoryImplTest {
    @Test
    fun pageUsesLookaheadRowToDetermineHasMore() = runTest {
        val repository = SeriesRepositoryImpl(
            FakeSeriesDataSource(rows = listOf(row("1"), row("2"), row("3"))),
        )

        val page = success(repository.getSeries(SeriesQuery(page = 2, pageSize = 2)))

        assertEquals(2, page.items.size)
        assertEquals(2, page.page)
        assertTrue(page.hasMore)
    }

    @Test
    fun publishedCreateAssignsPublishedAt() = runTest {
        val dataSource = FakeSeriesDataSource()
        val repository = SeriesRepositoryImpl(dataSource)

        success(repository.saveSeries(command(id = null, isPublished = true)))

        assertNotNull(dataSource.createdPayload?.publishedAt)
    }

    @Test
    fun unpublishPreservesExistingPublishedAt() = runTest {
        val dataSource = FakeSeriesDataSource()
        val repository = SeriesRepositoryImpl(dataSource)
        val existing = Instant.parse("2026-09-01T00:00:00Z")

        success(
            repository.saveSeries(
                command(id = "series-1", isPublished = false, publishedAt = existing),
            ),
        )

        assertEquals(existing.toString(), dataSource.updatedPayload?.publishedAt)
        assertEquals(false, dataSource.updatedPayload?.isPublished)
    }

    @Test
    fun duplicateSlugMapsToConflict() = runTest {
        val repository = SeriesRepositoryImpl(
            FakeSeriesDataSource(saveFailure = SeriesSlugConflictException()),
        )

        val result = repository.saveSeries(command(id = null))

        assertEquals(AppError.Conflict, (result as AppResult.Failure).error)
    }

    @Test
    fun softDeleteDelegatesToDataSource() = runTest {
        val dataSource = FakeSeriesDataSource()
        val repository = SeriesRepositoryImpl(dataSource)

        success(repository.softDeleteSeries("series-1"))

        assertEquals("series-1", dataSource.deletedId)
    }

    @Test
    fun configurationFailureIsMapped() = runTest {
        val repository = SeriesRepositoryImpl(
            FakeSeriesDataSource(readFailure = IllegalArgumentException("missing config")),
        )

        val result = repository.getSeries(SeriesQuery())

        assertEquals(AppError.Configuration, (result as AppResult.Failure).error)
    }

    private class FakeSeriesDataSource(
        private val rows: List<SeriesRowDto> = emptyList(),
        private val readFailure: Throwable? = null,
        private val saveFailure: Throwable? = null,
    ) : SeriesDataSource {
        var createdPayload: SeriesWriteDto? = null
        var updatedPayload: SeriesWriteDto? = null
        var deletedId: String? = null

        override suspend fun fetchSeries(query: SeriesQuery): List<SeriesRowDto> {
            readFailure?.let { throw it }
            return rows
        }

        override suspend fun fetchSeriesById(id: String): SeriesRowDto? =
            rows.firstOrNull { it.id == id }

        override suspend fun createSeries(payload: SeriesWriteDto): SeriesRowDto {
            saveFailure?.let { throw it }
            createdPayload = payload
            return payload.toRow()
        }

        override suspend fun updateSeries(payload: SeriesWriteDto): SeriesRowDto {
            saveFailure?.let { throw it }
            updatedPayload = payload
            return payload.toRow()
        }

        override suspend fun softDeleteSeries(id: String) {
            deletedId = id
        }
    }

    private fun command(
        id: String?,
        isPublished: Boolean = false,
        publishedAt: Instant? = null,
    ) = SeriesSaveCommand(
        id = id,
        slug = "wajah-kedua",
        title = "Wajah Kedua",
        shortSynopsis = null,
        synopsis = null,
        genres = listOf("Drama"),
        coverUrl = null,
        heroUrl = null,
        isFeatured = false,
        isPublished = isPublished,
        existingPublishedAt = publishedAt,
        seoTitle = null,
        seoDescription = null,
    )

    private fun SeriesWriteDto.toRow() = SeriesRowDto(
        id = id,
        slug = slug,
        title = title,
        shortSynopsis = shortSynopsis,
        synopsis = synopsis,
        genres = genres,
        coverUrl = coverUrl,
        heroUrl = heroUrl,
        isFeatured = isFeatured,
        isPublished = isPublished,
        publishedAt = publishedAt,
        seoTitle = seoTitle,
        seoDescription = seoDescription,
        createdAt = "2026-09-01T00:00:00Z",
        updatedAt = "2026-09-01T00:00:00Z",
    )

    private fun row(id: String) = SeriesRowDto(
        id = id,
        slug = "series-$id",
        title = "Series $id",
        isFeatured = false,
        isPublished = false,
        createdAt = "2026-09-01T00:00:00Z",
        updatedAt = "2026-09-01T00:00:00Z",
    )

    private fun <T> success(result: AppResult<T>): T =
        (result as AppResult.Success).value
}

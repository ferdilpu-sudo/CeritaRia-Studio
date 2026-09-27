package com.flyonz.ceritaria.studio.feature.series.data

import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesQuery
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesRepositoryImplTest {
    @Test
    fun pageUsesLookaheadRowToDetermineHasMore() = runTest {
        val dataSource = FakeSeriesDataSource(
            rows = listOf(row("1"), row("2"), row("3")),
        )
        val repository = SeriesRepositoryImpl(dataSource)

        val page = when (val result = repository.getSeries(SeriesQuery(page = 2, pageSize = 2))) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> error("Expected success, got ${result.error}")
        }

        assertEquals(2, page.items.size)
        assertEquals(2, page.page)
        assertTrue(page.hasMore)
    }

    @Test
    fun configurationFailureIsMapped() = runTest {
        val repository = SeriesRepositoryImpl(
            FakeSeriesDataSource(failure = IllegalArgumentException("missing config")),
        )

        val result = repository.getSeries(SeriesQuery())

        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.Configuration, (result as AppResult.Failure).error)
    }

    private class FakeSeriesDataSource(
        private val rows: List<SeriesRowDto> = emptyList(),
        private val failure: Throwable? = null,
    ) : SeriesDataSource {
        override suspend fun fetchSeries(query: SeriesQuery): List<SeriesRowDto> {
            failure?.let { throw it }
            return rows
        }

        override suspend fun fetchSeriesById(id: String): SeriesRowDto? =
            rows.firstOrNull { it.id == id }
    }

    private fun row(id: String) = SeriesRowDto(
        id = id,
        slug = "series-$id",
        title = "Series $id",
        isFeatured = false,
        isPublished = false,
        createdAt = "2026-09-01T00:00:00Z",
        updatedAt = "2026-09-01T00:00:00Z",
    )
}

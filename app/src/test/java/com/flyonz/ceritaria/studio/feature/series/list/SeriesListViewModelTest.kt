package com.flyonz.ceritaria.studio.feature.series.list

import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.core.model.PagedResult
import com.flyonz.ceritaria.studio.core.model.PublishStatus
import com.flyonz.ceritaria.studio.feature.series.domain.Series
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesQuery
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesRepository
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesSaveCommand
import com.flyonz.ceritaria.studio.testutil.MainDispatcherRule
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SeriesListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialLoadPopulatesState() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeSeriesRepository()
        val viewModel = SeriesListViewModel(repository)

        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(listOf("Series 1"), viewModel.state.value.items.map { it.title })
    }

    @Test
    fun searchWaitsForDebounceBeforeReloading() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeSeriesRepository()
        val viewModel = SeriesListViewModel(repository)
        advanceUntilIdle()
        val callsBeforeSearch = repository.queries.size

        viewModel.setQuery("wajah")
        runCurrent()
        advanceTimeBy(299)

        assertEquals(callsBeforeSearch, repository.queries.size)

        advanceTimeBy(2)
        advanceUntilIdle()

        assertEquals("wajah", repository.queries.last().search)
    }

    private class FakeSeriesRepository : SeriesRepository {
        val queries = mutableListOf<SeriesQuery>()

        override suspend fun getSeries(query: SeriesQuery): AppResult<PagedResult<Series>> {
            queries += query
            return AppResult.Success(
                PagedResult(
                    items = listOf(series()),
                    page = query.page,
                    hasMore = false,
                ),
            )
        }

        override suspend fun getSeriesById(id: String): AppResult<Series?> =
            AppResult.Success(series())

        override suspend fun saveSeries(command: SeriesSaveCommand): AppResult<Series> =
            AppResult.Failure(AppError.Unknown)

        override suspend fun softDeleteSeries(id: String): AppResult<Unit> =
            AppResult.Failure(AppError.Unknown)

        private fun series() = Series(
            id = "series-1",
            slug = "series-1",
            title = "Series 1",
            shortSynopsis = null,
            synopsis = null,
            genres = emptyList(),
            coverUrl = null,
            heroUrl = null,
            isFeatured = false,
            publishStatus = PublishStatus.DRAFT,
            publishedAt = null,
            seoTitle = null,
            seoDescription = null,
            createdAt = Instant.parse("2026-09-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-09-01T00:00:00Z"),
            deletedAt = null,
        )
    }
}

package com.flyonz.ceritaria.studio.feature.series.detail

import androidx.lifecycle.SavedStateHandle
import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.core.model.PagedResult
import com.flyonz.ceritaria.studio.feature.series.domain.Series
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesQuery
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesRepository
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesSaveCommand
import com.flyonz.ceritaria.studio.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SeriesDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun successfulDeleteEmitsDeletedEffect() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeSeriesRepository()
        val viewModel = SeriesDetailViewModel(
            SavedStateHandle(mapOf("seriesId" to "series-1")),
            repository,
        )
        advanceUntilIdle()
        val effect = async { viewModel.effects.first() }

        viewModel.delete()
        advanceUntilIdle()

        assertEquals("series-1", repository.deletedId)
        assertEquals(SeriesDetailEffect.Deleted, effect.await())
        assertFalse(viewModel.state.value.isDeleting)
    }

    @Test
    fun failedDeleteKeepsDetailAndShowsErrorState() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeSeriesRepository(deleteFails = true)
            val viewModel = SeriesDetailViewModel(
                SavedStateHandle(mapOf("seriesId" to "series-1")),
                repository,
            )
            advanceUntilIdle()

            viewModel.delete()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.deleteError)
            assertFalse(viewModel.state.value.isDeleting)
        }

    private class FakeSeriesRepository(
        private val deleteFails: Boolean = false,
    ) : SeriesRepository {
        var deletedId: String? = null

        override suspend fun getSeries(query: SeriesQuery): AppResult<PagedResult<Series>> =
            AppResult.Success(PagedResult(emptyList(), query.page, false))

        override suspend fun getSeriesById(id: String): AppResult<Series?> =
            AppResult.Success(null)

        override suspend fun saveSeries(command: SeriesSaveCommand): AppResult<Series> =
            AppResult.Failure(AppError.Unknown)

        override suspend fun softDeleteSeries(id: String): AppResult<Unit> {
            deletedId = id
            return if (deleteFails) {
                AppResult.Failure(AppError.Network)
            } else {
                AppResult.Success(Unit)
            }
        }
    }
}

package com.flyonz.ceritaria.studio.feature.episode.list

import androidx.lifecycle.SavedStateHandle
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.core.model.PagedResult
import com.flyonz.ceritaria.studio.feature.episode.domain.Episode
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeQuery
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeRepository
import com.flyonz.ceritaria.studio.feature.episode.domain.VideoProviderFilter
import com.flyonz.ceritaria.studio.feature.series.domain.Series
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesQuery
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesRepository
import com.flyonz.ceritaria.studio.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EpisodeListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun navigationSeriesIdIsUsedByInitialQuery() = runTest(mainDispatcherRule.testDispatcher) {
        val episodeRepository = FakeEpisodeRepository()
        val viewModel = EpisodeListViewModel(
            savedStateHandle = SavedStateHandle(mapOf("seriesId" to "series-1")),
            episodeRepository = episodeRepository,
            seriesRepository = EmptySeriesRepository(),
        )

        advanceUntilIdle()

        assertEquals("series-1", viewModel.state.value.seriesId)
        assertEquals("series-1", episodeRepository.queries.first().seriesId)
    }

    @Test
    fun providerFilterReloadsCatalog() = runTest(mainDispatcherRule.testDispatcher) {
        val episodeRepository = FakeEpisodeRepository()
        val viewModel = EpisodeListViewModel(
            savedStateHandle = SavedStateHandle(),
            episodeRepository = episodeRepository,
            seriesRepository = EmptySeriesRepository(),
        )
        advanceUntilIdle()

        viewModel.setProvider(VideoProviderFilter.FACEBOOK)
        advanceUntilIdle()

        assertEquals(VideoProviderFilter.FACEBOOK, episodeRepository.queries.last().provider)
    }

    private class FakeEpisodeRepository : EpisodeRepository {
        val queries = mutableListOf<EpisodeQuery>()

        override suspend fun getEpisodes(query: EpisodeQuery): AppResult<PagedResult<Episode>> {
            queries += query
            return AppResult.Success(PagedResult(emptyList(), query.page, false))
        }

        override suspend fun getEpisodeById(id: String): AppResult<Episode?> =
            AppResult.Success(null)
    }

    private class EmptySeriesRepository : SeriesRepository {
        override suspend fun getSeries(query: SeriesQuery): AppResult<PagedResult<Series>> =
            AppResult.Success(PagedResult(emptyList(), query.page, false))

        override suspend fun getSeriesById(id: String): AppResult<Series?> =
            AppResult.Success(null)
    }
}

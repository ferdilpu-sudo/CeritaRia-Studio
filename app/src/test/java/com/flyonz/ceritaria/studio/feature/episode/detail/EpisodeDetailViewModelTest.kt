package com.flyonz.ceritaria.studio.feature.episode.detail

import androidx.lifecycle.SavedStateHandle
import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.core.model.PagedResult
import com.flyonz.ceritaria.studio.feature.episode.domain.Episode
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeQuery
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeRepository
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeSaveCommand
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
class EpisodeDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun successfulDeleteEmitsDeletedEffect() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeEpisodeRepository()
        val viewModel = EpisodeDetailViewModel(
            SavedStateHandle(mapOf("episodeId" to "episode-1")),
            repository,
        )
        advanceUntilIdle()
        val effect = async { viewModel.effects.first() }

        viewModel.delete()
        advanceUntilIdle()

        assertEquals("episode-1", repository.deletedId)
        assertEquals(EpisodeDetailEffect.Deleted, effect.await())
        assertFalse(viewModel.state.value.isDeleting)
    }

    @Test
    fun failedDeleteSetsVisibleErrorState() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeEpisodeRepository(deleteFails = true)
        val viewModel = EpisodeDetailViewModel(
            SavedStateHandle(mapOf("episodeId" to "episode-1")),
            repository,
        )
        advanceUntilIdle()

        viewModel.delete()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.deleteError)
        assertFalse(viewModel.state.value.isDeleting)
    }

    private class FakeEpisodeRepository(
        private val deleteFails: Boolean = false,
    ) : EpisodeRepository {
        var deletedId: String? = null

        override suspend fun getEpisodes(query: EpisodeQuery): AppResult<PagedResult<Episode>> =
            AppResult.Success(PagedResult(emptyList(), query.page, false))

        override suspend fun getEpisodeById(id: String): AppResult<Episode?> =
            AppResult.Success(null)

        override suspend fun getEpisodesForReorder(seriesId: String): AppResult<List<Episode>> =
            AppResult.Success(emptyList())

        override suspend fun saveEpisode(command: EpisodeSaveCommand): AppResult<Episode> =
            AppResult.Failure(AppError.Unknown)

        override suspend fun softDeleteEpisode(id: String): AppResult<Unit> {
            deletedId = id
            return if (deleteFails) {
                AppResult.Failure(AppError.Network)
            } else {
                AppResult.Success(Unit)
            }
        }

        override suspend fun reorderEpisodes(
            seriesId: String,
            orderedEpisodeIds: List<String>,
        ): AppResult<Unit> = AppResult.Success(Unit)
    }
}

package com.flyonz.ceritaria.studio.feature.episode.reorder

import androidx.lifecycle.SavedStateHandle
import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.core.model.PagedResult
import com.flyonz.ceritaria.studio.core.model.PublishStatus
import com.flyonz.ceritaria.studio.feature.episode.domain.Episode
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeQuery
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeRepository
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeSaveCommand
import com.flyonz.ceritaria.studio.feature.episode.domain.VideoProvider
import com.flyonz.ceritaria.studio.testutil.MainDispatcherRule
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EpisodeReorderViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun moveDownChangesLocalOrderAndDirtyState() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeEpisodeRepository(
            mutableListOf(episode("1", 1), episode("2", 2), episode("3", 3)),
        )
        val viewModel = EpisodeReorderViewModel(
            SavedStateHandle(mapOf("seriesId" to SERIES_ID)),
            repository,
        )
        advanceUntilIdle()

        viewModel.moveDown(0)

        assertEquals(listOf("2", "1", "3"), viewModel.state.value.episodes.map { it.id })
        assertTrue(viewModel.state.value.isDirty)
    }

    @Test
    fun saveSendsSingleOrderedListAndRefreshesCleanState() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeEpisodeRepository(
                mutableListOf(episode("1", 1), episode("2", 2), episode("3", 3)),
            )
            val viewModel = EpisodeReorderViewModel(
                SavedStateHandle(mapOf("seriesId" to SERIES_ID)),
                repository,
            )
            advanceUntilIdle()
            viewModel.moveDown(0)

            viewModel.save()
            advanceUntilIdle()

            assertEquals(listOf("2", "1", "3"), repository.lastReorderIds)
            assertFalse(viewModel.state.value.isDirty)
            assertTrue(viewModel.state.value.saveSucceeded)
        }

    @Test
    fun conflictSurfacesRetryableSaveError() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeEpisodeRepository(
            mutableListOf(episode("1", 1), episode("2", 2)),
            reorderResult = AppResult.Failure(AppError.Conflict),
        )
        val viewModel = EpisodeReorderViewModel(
            SavedStateHandle(mapOf("seriesId" to SERIES_ID)),
            repository,
        )
        advanceUntilIdle()
        viewModel.moveDown(0)

        viewModel.save()
        advanceUntilIdle()

        assertEquals(EpisodeReorderSaveError.CONFLICT, viewModel.state.value.saveError)
        assertTrue(viewModel.state.value.isDirty)
    }

    private class FakeEpisodeRepository(
        private val episodes: MutableList<Episode>,
        private val reorderResult: AppResult<Unit> = AppResult.Success(Unit),
    ) : EpisodeRepository {
        var lastReorderIds: List<String>? = null

        override suspend fun getEpisodes(query: EpisodeQuery): AppResult<PagedResult<Episode>> =
            AppResult.Success(PagedResult(emptyList(), query.page, false))

        override suspend fun getEpisodeById(id: String): AppResult<Episode?> =
            AppResult.Success(episodes.firstOrNull { it.id == id })

        override suspend fun getEpisodesForReorder(seriesId: String): AppResult<List<Episode>> =
            AppResult.Success(episodes.toList())

        override suspend fun saveEpisode(command: EpisodeSaveCommand): AppResult<Episode> =
            AppResult.Failure(AppError.Unknown)

        override suspend fun softDeleteEpisode(id: String): AppResult<Unit> =
            AppResult.Success(Unit)

        override suspend fun reorderEpisodes(
            seriesId: String,
            orderedEpisodeIds: List<String>,
        ): AppResult<Unit> {
            lastReorderIds = orderedEpisodeIds
            if (reorderResult is AppResult.Success) {
                val byId = episodes.associateBy { it.id }
                val reordered = orderedEpisodeIds.mapNotNull(byId::get)
                episodes.clear()
                episodes.addAll(reordered)
            }
            return reorderResult
        }
    }

    private companion object {
        const val SERIES_ID = "11111111-1111-1111-1111-111111111111"
    }
}

private fun episode(id: String, number: Int) = Episode(
    id = id,
    seriesId = "11111111-1111-1111-1111-111111111111",
    seriesTitle = "Wajah Kedua",
    episodeNumber = number,
    slug = "episode-$id",
    title = "Episode $id",
    shortSynopsis = null,
    recap = null,
    highlights = emptyList(),
    videoProvider = VideoProvider.YouTube,
    videoUrl = "https://youtu.be/dQw4w9WgXcQ",
    thumbnailUrl = null,
    durationSeconds = null,
    publishStatus = PublishStatus.DRAFT,
    publishedAt = null,
    seoTitle = null,
    seoDescription = null,
    createdAt = Instant.parse("2026-09-01T00:00:00Z"),
    updatedAt = Instant.parse("2026-09-01T00:00:00Z"),
    deletedAt = null,
)

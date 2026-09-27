package com.flyonz.ceritaria.studio.feature.episode.editor

import androidx.lifecycle.SavedStateHandle
import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.core.model.PagedResult
import com.flyonz.ceritaria.studio.core.model.PublishStatus
import com.flyonz.ceritaria.studio.feature.episode.domain.EditableVideoProvider
import com.flyonz.ceritaria.studio.feature.episode.domain.Episode
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeQuery
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeRepository
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeSaveCommand
import com.flyonz.ceritaria.studio.feature.episode.domain.VideoProvider
import com.flyonz.ceritaria.studio.feature.series.domain.Series
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesQuery
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesRepository
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesSaveCommand
import com.flyonz.ceritaria.studio.testutil.MainDispatcherRule
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EpisodeEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun preselectedSeriesIsLoadedEvenWhenOutsideBaseOptions() =
        runTest(mainDispatcherRule.testDispatcher) {
            val requiredId = REQUIRED_SERIES_ID
            val viewModel = EpisodeEditorViewModel(
                SavedStateHandle(mapOf("seriesId" to requiredId)),
                FakeEpisodeRepository(),
                FakeSeriesRepository(base = listOf(series(OTHER_SERIES_ID)), required = series(requiredId)),
            )

            advanceUntilIdle()

            assertEquals(requiredId, viewModel.state.value.form.seriesId)
            assertEquals(requiredId, viewModel.state.value.seriesOptions.last().id)
        }

    @Test
    fun successfulCreateClearsDirtyState() = runTest(mainDispatcherRule.testDispatcher) {
        val episodeRepository = FakeEpisodeRepository()
        val viewModel = EpisodeEditorViewModel(
            SavedStateHandle(mapOf("seriesId" to REQUIRED_SERIES_ID)),
            episodeRepository,
            FakeSeriesRepository(base = listOf(series(REQUIRED_SERIES_ID))),
        )
        advanceUntilIdle()
        viewModel.setForm(validForm())

        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, episodeRepository.saveCalls)
        assertFalse(viewModel.state.value.isDirty)
    }

    @Test
    fun conflictMapsToEditorError() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = EpisodeEditorViewModel(
            SavedStateHandle(mapOf("seriesId" to REQUIRED_SERIES_ID)),
            FakeEpisodeRepository(saveResult = AppResult.Failure(AppError.Conflict)),
            FakeSeriesRepository(base = listOf(series(REQUIRED_SERIES_ID))),
        )
        advanceUntilIdle()
        viewModel.setForm(validForm())

        viewModel.save()
        advanceUntilIdle()

        assertEquals(EpisodeEditorSaveError.CONFLICT, viewModel.state.value.saveError)
    }

    private class FakeEpisodeRepository(
        private val saveResult: AppResult<Episode>? = null,
    ) : EpisodeRepository {
        var saveCalls = 0

        override suspend fun getEpisodes(query: EpisodeQuery): AppResult<PagedResult<Episode>> =
            AppResult.Success(PagedResult(emptyList(), query.page, false))

        override suspend fun getEpisodeById(id: String): AppResult<Episode?> =
            AppResult.Success(null)

        override suspend fun getEpisodesForReorder(seriesId: String): AppResult<List<Episode>> =
            AppResult.Success(emptyList())

        override suspend fun saveEpisode(command: EpisodeSaveCommand): AppResult<Episode> {
            saveCalls += 1
            return saveResult ?: AppResult.Success(episode(command))
        }

        override suspend fun softDeleteEpisode(id: String): AppResult<Unit> =
            AppResult.Success(Unit)

        override suspend fun reorderEpisodes(
            seriesId: String,
            orderedEpisodeIds: List<String>,
        ): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeSeriesRepository(
        private val base: List<Series>,
        private val required: Series? = null,
    ) : SeriesRepository {
        override suspend fun getSeries(query: SeriesQuery): AppResult<PagedResult<Series>> =
            AppResult.Success(PagedResult(base, query.page, false))

        override suspend fun getSeriesById(id: String): AppResult<Series?> =
            AppResult.Success(required?.takeIf { it.id == id } ?: base.firstOrNull { it.id == id })

        override suspend fun saveSeries(command: SeriesSaveCommand): AppResult<Series> =
            AppResult.Failure(AppError.Unknown)

        override suspend fun softDeleteSeries(id: String): AppResult<Unit> =
            AppResult.Failure(AppError.Unknown)
    }

    private fun validForm() = EpisodeEditorForm(
        seriesId = REQUIRED_SERIES_ID,
        episodeNumber = "1",
        slug = "episode-1",
        title = "Episode 1",
        videoProvider = "youtube",
        videoUrl = "https://youtu.be/dQw4w9WgXcQ",
    )

    private companion object {
        const val REQUIRED_SERIES_ID = "11111111-1111-1111-1111-111111111111"
        const val OTHER_SERIES_ID = "22222222-2222-2222-2222-222222222222"
    }
}

private fun series(id: String) = Series(
    id = id,
    slug = "series-$id",
    title = "Series $id",
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

private fun episode(command: EpisodeSaveCommand) = Episode(
    id = command.id ?: "33333333-3333-3333-3333-333333333333",
    seriesId = command.seriesId,
    seriesTitle = null,
    episodeNumber = command.episodeNumber,
    slug = command.slug,
    title = command.title,
    shortSynopsis = command.shortSynopsis,
    recap = command.recap,
    highlights = command.highlights,
    videoProvider = when (command.videoProvider) {
        EditableVideoProvider.YOUTUBE -> VideoProvider.YouTube
        EditableVideoProvider.FACEBOOK -> VideoProvider.Facebook
    },
    videoUrl = command.videoUrl,
    thumbnailUrl = command.thumbnailUrl,
    durationSeconds = command.durationSeconds,
    publishStatus = if (command.isPublished) PublishStatus.PUBLISHED else PublishStatus.DRAFT,
    publishedAt = command.existingPublishedAt,
    seoTitle = command.seoTitle,
    seoDescription = command.seoDescription,
    createdAt = Instant.parse("2026-09-01T00:00:00Z"),
    updatedAt = Instant.parse("2026-09-01T00:00:00Z"),
    deletedAt = null,
)

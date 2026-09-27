package com.flyonz.ceritaria.studio.feature.series.editor

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SeriesEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun invalidFormDoesNotCallRepository() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeSeriesRepository()
        val viewModel = SeriesEditorViewModel(SavedStateHandle(), repository)

        viewModel.save()
        advanceUntilIdle()

        assertEquals(0, repository.saveCalls)
        assertTrue(viewModel.state.value.validationErrors.containsKey(SeriesEditorField.TITLE))
        assertTrue(viewModel.state.value.validationErrors.containsKey(SeriesEditorField.SLUG))
    }

    @Test
    fun successfulCreateNormalizesFormAndClearsDirtyState() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeSeriesRepository()
            val viewModel = SeriesEditorViewModel(SavedStateHandle(), repository)
            viewModel.setForm(validForm().copy(title = "  Wajah Kedua  "))

            viewModel.save()
            advanceUntilIdle()

            assertEquals(1, repository.saveCalls)
            assertEquals("Wajah Kedua", viewModel.state.value.form.title)
            assertFalse(viewModel.state.value.isDirty)
        }

    @Test
    fun duplicateSaveWhileSubmittingIsIgnored() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeSeriesRepository()
        val viewModel = SeriesEditorViewModel(SavedStateHandle(), repository)
        viewModel.setForm(validForm())

        viewModel.save()
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, repository.saveCalls)
    }

    @Test
    fun duplicateSlugMapsToEditorError() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeSeriesRepository(
            saveResult = AppResult.Failure(AppError.Conflict),
        )
        val viewModel = SeriesEditorViewModel(SavedStateHandle(), repository)
        viewModel.setForm(validForm())

        viewModel.save()
        advanceUntilIdle()

        assertEquals(SeriesEditorSaveError.DUPLICATE_SLUG, viewModel.state.value.saveError)
    }

    @Test
    fun editLoadsExistingSeriesAsCleanForm() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeSeriesRepository(existing = series())
        val viewModel = SeriesEditorViewModel(
            SavedStateHandle(mapOf("seriesId" to "series-1")),
            repository,
        )

        advanceUntilIdle()

        assertEquals("Wajah Kedua", viewModel.state.value.form.title)
        assertTrue(viewModel.state.value.isEdit)
        assertFalse(viewModel.state.value.isDirty)
    }

    private inner class FakeSeriesRepository(
        private val existing: Series? = null,
        private val saveResult: AppResult<Series>? = null,
    ) : SeriesRepository {
        var saveCalls = 0

        override suspend fun getSeries(query: SeriesQuery): AppResult<PagedResult<Series>> =
            AppResult.Success(PagedResult(emptyList(), query.page, false))

        override suspend fun getSeriesById(id: String): AppResult<Series?> =
            AppResult.Success(existing)

        override suspend fun saveSeries(command: SeriesSaveCommand): AppResult<Series> {
            saveCalls += 1
            return saveResult ?: AppResult.Success(series(command))
        }

        override suspend fun softDeleteSeries(id: String): AppResult<Unit> =
            AppResult.Success(Unit)
    }

    private fun validForm() = SeriesEditorForm(
        slug = "wajah-kedua",
        title = "Wajah Kedua",
    )

    private fun series(command: SeriesSaveCommand? = null) = Series(
        id = command?.id ?: "series-1",
        slug = command?.slug ?: "wajah-kedua",
        title = command?.title ?: "Wajah Kedua",
        shortSynopsis = command?.shortSynopsis,
        synopsis = command?.synopsis,
        genres = command?.genres ?: emptyList(),
        coverUrl = command?.coverUrl,
        heroUrl = command?.heroUrl,
        isFeatured = command?.isFeatured ?: false,
        publishStatus = if (command?.isPublished == true) {
            PublishStatus.PUBLISHED
        } else {
            PublishStatus.DRAFT
        },
        publishedAt = command?.existingPublishedAt,
        seoTitle = command?.seoTitle,
        seoDescription = command?.seoDescription,
        createdAt = Instant.parse("2026-09-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-09-01T00:00:00Z"),
        deletedAt = null,
    )
}

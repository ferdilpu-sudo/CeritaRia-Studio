package com.flyonz.ceritaria.studio.feature.episode.editor.video

import androidx.lifecycle.SavedStateHandle
import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeVideoAssetRepository
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeVideoAttachment
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeVideoPreview
import com.flyonz.ceritaria.studio.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EpisodeVideoAttachmentViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun successfulAttachPublishesEffectAndAttachedState() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = EpisodeVideoAttachmentViewModel(
                SavedStateHandle(mapOf("episodeId" to "episode-1")),
                FakeRepository(
                    attachResult = AppResult.Success(
                        EpisodeVideoAttachment(
                            episodeId = "episode-1",
                            assetId = "asset-1",
                            replacedAssetId = "asset-old",
                        ),
                    ),
                ),
            )
            val effect = async { viewModel.effects.first() }

            viewModel.attach("asset-1")
            advanceUntilIdle()

            assertEquals(
                EpisodeVideoAttachmentStatus.ATTACHED,
                viewModel.state.value.status,
            )
            assertEquals("asset-1", viewModel.state.value.assetId)
            assertEquals(
                EpisodeVideoAttachmentEffect.Attached("asset-1", "asset-old"),
                effect.await(),
            )
        }

    @Test
    fun duplicateAttachWhileSubmittingIsIgnored() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeRepository(
                attachResult = AppResult.Success(
                    EpisodeVideoAttachment(
                        episodeId = "episode-1",
                        assetId = "asset-1",
                        replacedAssetId = null,
                    ),
                ),
            )
            val viewModel = EpisodeVideoAttachmentViewModel(
                SavedStateHandle(mapOf("episodeId" to "episode-1")),
                repository,
            )

            viewModel.attach("asset-1")
            viewModel.attach("asset-1")
            advanceUntilIdle()

            assertEquals(1, repository.attachCalls)
        }

    @Test
    fun conflictBecomesRetryableAttachmentFailure() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = EpisodeVideoAttachmentViewModel(
                SavedStateHandle(mapOf("episodeId" to "episode-1")),
                FakeRepository(attachResult = AppResult.Failure(AppError.Conflict)),
            )

            viewModel.attach("asset-1")
            advanceUntilIdle()

            assertEquals(
                EpisodeVideoAttachmentStatus.FAILED,
                viewModel.state.value.status,
            )
            assertEquals("NOT_ATTACHABLE", viewModel.state.value.errorCode)
        }

    @Test
    fun duplicatePreviewWhileLoadingIsIgnored() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeRepository(
                attachResult = AppResult.Failure(AppError.Unknown),
                previewResult = AppResult.Success(
                    EpisodeVideoPreview(
                        assetId = "asset-1",
                        url = "https://preview.example/video",
                        expiresInSeconds = 900,
                    ),
                ),
            )
            val viewModel = EpisodeVideoAttachmentViewModel(
                SavedStateHandle(mapOf("episodeId" to "episode-1")),
                repository,
            )

            viewModel.preview("asset-1")
            viewModel.preview("asset-1")
            advanceUntilIdle()

            assertEquals(1, repository.previewCalls)
        }

    @Test
    fun previewPublishesTemporaryUrlEffect() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = EpisodeVideoAttachmentViewModel(
                SavedStateHandle(mapOf("episodeId" to "episode-1")),
                FakeRepository(
                    attachResult = AppResult.Failure(AppError.Unknown),
                    previewResult = AppResult.Success(
                        EpisodeVideoPreview(
                            assetId = "asset-1",
                            url = "https://preview.example/video",
                            expiresInSeconds = 900,
                        ),
                    ),
                ),
            )
            val effect = async { viewModel.effects.first() }

            viewModel.preview("asset-1")
            advanceUntilIdle()

            assertEquals(
                EpisodeVideoAttachmentEffect.PreviewReady(
                    "https://preview.example/video",
                ),
                effect.await(),
            )
            assertEquals(
                EpisodeVideoPreviewStatus.IDLE,
                viewModel.state.value.previewStatus,
            )
        }

    private class FakeRepository(
        private val attachResult: AppResult<EpisodeVideoAttachment>,
        private val previewResult: AppResult<EpisodeVideoPreview> =
            AppResult.Failure(AppError.Unknown),
    ) : EpisodeVideoAssetRepository {
        var attachCalls = 0
        var previewCalls = 0

        override suspend fun attachReadyAsset(
            episodeId: String,
            assetId: String,
        ): AppResult<EpisodeVideoAttachment> {
            attachCalls += 1
            return attachResult
        }

        override suspend fun getPreview(assetId: String): AppResult<EpisodeVideoPreview> {
            previewCalls += 1
            return previewResult
        }
    }
}

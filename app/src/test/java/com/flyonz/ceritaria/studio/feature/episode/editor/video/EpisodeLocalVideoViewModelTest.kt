package com.flyonz.ceritaria.studio.feature.episode.editor.video

import androidx.lifecycle.SavedStateHandle
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobFactory
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobRepository
import com.flyonz.ceritaria.studio.core.media.EncodingProgress
import com.flyonz.ceritaria.studio.core.media.TemporaryMediaStore
import com.flyonz.ceritaria.studio.core.media.VideoCompatibilityChecker
import com.flyonz.ceritaria.studio.core.media.VideoEncodeResult
import com.flyonz.ceritaria.studio.core.media.VideoEncoder
import com.flyonz.ceritaria.studio.core.media.VideoEncodingCoordinator
import com.flyonz.ceritaria.studio.core.media.VideoEncodingRecovery
import com.flyonz.ceritaria.studio.core.media.VideoEncodingStatus
import com.flyonz.ceritaria.studio.core.media.VideoInspector
import com.flyonz.ceritaria.studio.core.media.VideoMetadata
import com.flyonz.ceritaria.studio.core.media.VideoSelectionPreparer
import com.flyonz.ceritaria.studio.core.media.VideoSourceAccess
import com.flyonz.ceritaria.studio.core.upload.VideoUploadStatus
import com.flyonz.ceritaria.studio.testutil.MainDispatcherRule
import java.io.File
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EpisodeLocalVideoViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun newEpisodeRequiresSaveBeforeLocalVideo() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repo = FakeJobs()
            val viewModel = viewModel(repo, SavedStateHandle())

            advanceUntilIdle()

            assertEquals(EpisodeLocalVideoStatus.SAVE_FIRST, viewModel.state.value.status)
        }

    @Test
    fun staleEncodingRecoversOnceButLiveEncodingIsNotReset() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repo = FakeJobs(job(VideoEncodingStatus.ENCODING))
            val viewModel = viewModel(
                repo,
                SavedStateHandle(mapOf("episodeId" to "episode-1")),
            )

            advanceUntilIdle()

            assertEquals(EpisodeLocalVideoStatus.READY_TO_ENCODE, viewModel.state.value.status)
            repo.emit(job(VideoEncodingStatus.ENCODING))
            advanceUntilIdle()

            assertEquals(EpisodeLocalVideoStatus.ENCODING, viewModel.state.value.status)
        }

    @Test
    fun compatibleSelectionBecomesReadyWithoutEncoding() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repo = FakeJobs()
            val viewModel = viewModel(
                repo,
                SavedStateHandle(mapOf("episodeId" to "episode-1")),
            )

            viewModel.select("content://video/compatible")
            advanceUntilIdle()

            assertEquals(
                EpisodeLocalVideoStatus.READY_WITHOUT_ENCODING,
                viewModel.state.value.status,
            )
        }

    private fun viewModel(
        repo: FakeJobs,
        state: SavedStateHandle,
    ): EpisodeLocalVideoViewModel {
        val store = FakeStore()
        val preparer = VideoSelectionPreparer(
            sourceAccess = FakeAccess(),
            inspector = FakeInspector(compatibleMetadata()),
            jobFactory = VideoJobFactory(VideoCompatibilityChecker()),
            jobs = repo,
        )
        return EpisodeLocalVideoViewModel(
            savedStateHandle = state,
            selectionPreparer = preparer,
            encodingCoordinator = VideoEncodingCoordinator(
                jobs = repo,
                encoder = FakeEncoder(),
                temporaryMediaStore = store,
            ),
            encodingRecovery = VideoEncodingRecovery(repo, store),
            jobs = repo,
        )
    }

    private class FakeAccess : VideoSourceAccess {
        override fun persistReadAccess(sourceUri: String) = Unit
    }

    private class FakeInspector(
        private val metadata: VideoMetadata,
    ) : VideoInspector {
        override suspend fun inspect(sourceUri: String): VideoMetadata = metadata
    }

    private class FakeEncoder : VideoEncoder {
        override suspend fun encode(
            sourceUri: String,
            metadata: VideoMetadata,
            outputFile: File,
            onProgress: suspend (EncodingProgress) -> Unit,
        ): VideoEncodeResult {
            onProgress(EncodingProgress(100))
            return VideoEncodeResult(outputFile.toURI().toString(), 5_000_000)
        }
    }

    private class FakeStore : TemporaryMediaStore {
        override fun encodedOutput(jobId: String): File = File("$jobId.mp4")
        override fun hasEncodedOutput(jobId: String): Boolean = true
        override fun hasCapacity(estimatedOutputBytes: Long): Boolean = true
        override fun deleteEncodedOutput(jobId: String) = Unit
    }

    private class FakeJobs(
        initial: VideoJob? = null,
    ) : VideoJobRepository {
        private val latest = MutableStateFlow(initial)
        private var current = initial

        override suspend fun getById(jobId: String): VideoJob? = current

        override fun observeById(jobId: String): Flow<VideoJob?> = latest

        override fun observeLatestForEpisode(episodeId: String): Flow<VideoJob?> = latest

        override suspend fun upsert(job: VideoJob) {
            current = job
            latest.value = job
        }

        override suspend fun delete(jobId: String) {
            current = null
            latest.value = null
        }

        fun emit(job: VideoJob) {
            current = job
            latest.value = job
        }
    }

    private fun compatibleMetadata() = VideoMetadata(
        containerMimeType = "video/mp4",
        sizeBytes = 8_000_000,
        durationMs = 30_000,
        width = 1080,
        height = 1920,
        rotationDegrees = 0,
        frameRate = 30f,
        videoMimeType = "video/avc",
        audioMimeType = "audio/mp4a-latm",
        audioBitrate = 192_000,
        audioSampleRate = 48_000,
        hasVideo = true,
        hasAudio = true,
    )

    private fun job(status: VideoEncodingStatus) = VideoJob(
        jobId = "job-1",
        episodeId = "episode-1",
        sourceUri = "content://video/1",
        sourceFingerprint = "fingerprint",
        sourceMetadata = compatibleMetadata().copy(videoMimeType = "video/hevc"),
        needsEncoding = true,
        encodedLocalUri = null,
        encodingStatus = status,
        encodingProgress = if (status == VideoEncodingStatus.ENCODING) 40 else 0,
        uploadStatus = VideoUploadStatus.NOT_STARTED,
        uploadedBytes = 0,
        totalBytes = 8_000_000,
        uploadSessionId = null,
        multipartState = null,
        remoteAssetId = null,
        lastErrorCode = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}

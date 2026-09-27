package com.flyonz.ceritaria.studio.core.media

import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobRepository
import com.flyonz.ceritaria.studio.core.upload.VideoUploadStatus
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoEncodingCoordinatorTest {
    @Test
    fun successfulEncodePersistsProgressAndReadyOutput() = runTest {
        val repository = FakeJobs(job())
        val coordinator = VideoEncodingCoordinator(
            jobs = repository,
            encoder = FakeEncoder(),
            temporaryMediaStore = FakeStore(),
        )

        val result = coordinator.encode("job-1")

        assertEquals(VideoEncodingStatus.READY, result.encodingStatus)
        assertEquals(100, result.encodingProgress)
        assertEquals("file:/encoded/job-1.mp4", result.encodedLocalUri)
        assertEquals(5_000_000, result.totalBytes)
        assertTrue(repository.saved.any { it.encodingProgress == 55 })
    }

    @Test
    fun insufficientStorageFailsBeforeEncoderStarts() = runTest {
        val repository = FakeJobs(job())
        val encoder = FakeEncoder()
        val coordinator = VideoEncodingCoordinator(
            jobs = repository,
            encoder = encoder,
            temporaryMediaStore = FakeStore(hasCapacity = false),
        )

        runCatching { coordinator.encode("job-1") }

        assertEquals(0, encoder.calls)
        assertEquals(VideoEncodingStatus.FAILED, repository.current.encodingStatus)
        assertEquals(
            VideoEncodingErrorCode.INSUFFICIENT_STORAGE.name,
            repository.current.lastErrorCode,
        )
    }

    @Test
    fun unsupportedCodecIsPersistedDistinctly() = runTest {
        val repository = FakeJobs(job())
        val coordinator = VideoEncodingCoordinator(
            jobs = repository,
            encoder = FakeEncoder(
                failure = VideoEncoderException(VideoEncoderErrorCode.UNSUPPORTED_CODEC),
            ),
            temporaryMediaStore = FakeStore(),
        )

        runCatching { coordinator.encode("job-1") }

        assertEquals(VideoEncodingStatus.FAILED, repository.current.encodingStatus)
        assertEquals(
            VideoEncodingErrorCode.UNSUPPORTED_CODEC.name,
            repository.current.lastErrorCode,
        )
    }

    @Test
    fun cancellationIsPersistedAndRethrown() = runTest {
        val repository = FakeJobs(job())
        val coordinator = VideoEncodingCoordinator(
            jobs = repository,
            encoder = FakeEncoder(cancel = true),
            temporaryMediaStore = FakeStore(),
        )

        var cancelled = false
        try {
            coordinator.encode("job-1")
        } catch (_: CancellationException) {
            cancelled = true
        }

        assertTrue(cancelled)
        assertEquals(VideoEncodingStatus.CANCELLED, repository.current.encodingStatus)
    }

    private class FakeEncoder(
        private val cancel: Boolean = false,
        private val failure: Throwable? = null,
    ) : VideoEncoder {
        var calls = 0

        override suspend fun encode(
            sourceUri: String,
            metadata: VideoMetadata,
            outputFile: File,
            onProgress: suspend (EncodingProgress) -> Unit,
        ): VideoEncodeResult {
            calls += 1
            onProgress(EncodingProgress(55))
            if (cancel) throw CancellationException("cancelled")
            failure?.let { throw it }
            return VideoEncodeResult(
                outputUri = "file:/encoded/job-1.mp4",
                sizeBytes = 5_000_000,
            )
        }
    }

    private class FakeStore(
        private val hasCapacity: Boolean = true,
    ) : TemporaryMediaStore {
        override fun encodedOutput(jobId: String): File = File("$jobId.mp4")
        override fun hasEncodedOutput(jobId: String): Boolean = true
        override fun hasCapacity(estimatedOutputBytes: Long): Boolean = hasCapacity
        override fun deleteEncodedOutput(jobId: String) = Unit
    }

    private class FakeJobs(initial: VideoJob) : VideoJobRepository {
        var current = initial
        val saved = mutableListOf<VideoJob>()

        override suspend fun getById(jobId: String): VideoJob? = current
        override fun observeById(jobId: String): Flow<VideoJob?> = emptyFlow()
        override fun observeLatestForEpisode(episodeId: String): Flow<VideoJob?> = emptyFlow()
        override suspend fun upsert(job: VideoJob) {
            current = job
            saved += job
        }
        override suspend fun delete(jobId: String) = Unit
    }

    private fun job() = VideoJob(
        jobId = "job-1",
        episodeId = "episode-1",
        sourceUri = "content://video/1",
        sourceFingerprint = "fingerprint",
        sourceMetadata = VideoMetadata(
            containerMimeType = "video/mp4",
            sizeBytes = 12_000_000,
            durationMs = 30_000,
            width = 2160,
            height = 3840,
            rotationDegrees = 0,
            frameRate = 30f,
            videoMimeType = "video/hevc",
            audioMimeType = "audio/mp4a-latm",
            audioBitrate = 128_000,
            audioSampleRate = 48_000,
            hasVideo = true,
            hasAudio = true,
        ),
        needsEncoding = true,
        encodedLocalUri = null,
        encodingStatus = VideoEncodingStatus.QUEUED,
        encodingProgress = 0,
        uploadStatus = VideoUploadStatus.NOT_STARTED,
        uploadedBytes = 0,
        totalBytes = 12_000_000,
        uploadSessionId = null,
        multipartState = null,
        remoteAssetId = null,
        lastErrorCode = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}

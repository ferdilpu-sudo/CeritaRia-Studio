package com.flyonz.ceritaria.studio.core.media

import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobRepository
import com.flyonz.ceritaria.studio.core.upload.VideoUploadStatus
import java.io.File
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoEncodingRecoveryTest {
    @Test
    fun interruptedEncodingReturnsToQueued() = runTest {
        val repository = FakeJobs()
        val recovery = VideoEncodingRecovery(
            repository,
            FakeStore(hasOutput = true),
        )

        val result = recovery.recover(job(VideoEncodingStatus.ENCODING))

        assertEquals(VideoEncodingStatus.QUEUED, result.encodingStatus)
        assertEquals(0, result.encodingProgress)
        assertEquals("ENCODING_INTERRUPTED", result.lastErrorCode)
    }

    @Test
    fun readyJobWithMissingFileReturnsToQueued() = runTest {
        val repository = FakeJobs()
        val recovery = VideoEncodingRecovery(
            repository,
            FakeStore(hasOutput = false),
        )

        val result = recovery.recover(job(VideoEncodingStatus.READY))

        assertEquals(VideoEncodingStatus.QUEUED, result.encodingStatus)
        assertEquals("ENCODED_OUTPUT_MISSING", result.lastErrorCode)
    }

    private class FakeStore(
        private val hasOutput: Boolean,
    ) : TemporaryMediaStore {
        override fun encodedOutput(jobId: String): File = File("$jobId.mp4")
        override fun hasEncodedOutput(jobId: String): Boolean = hasOutput
        override fun hasCapacity(estimatedOutputBytes: Long): Boolean = true
        override fun deleteEncodedOutput(jobId: String) = Unit
    }

    private class FakeJobs : VideoJobRepository {
        override suspend fun getById(jobId: String): VideoJob? = null
        override fun observeById(jobId: String): Flow<VideoJob?> = emptyFlow()
        override fun observeLatestForEpisode(episodeId: String): Flow<VideoJob?> = emptyFlow()
        override suspend fun upsert(job: VideoJob) = Unit
        override suspend fun delete(jobId: String) = Unit
    }

    private fun job(status: VideoEncodingStatus) = VideoJob(
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
        encodedLocalUri = if (status == VideoEncodingStatus.READY) "file:/encoded/job-1.mp4" else null,
        encodingStatus = status,
        encodingProgress = if (status == VideoEncodingStatus.READY) 100 else 35,
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

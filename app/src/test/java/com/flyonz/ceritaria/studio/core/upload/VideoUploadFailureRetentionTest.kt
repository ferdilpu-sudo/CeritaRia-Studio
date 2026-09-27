package com.flyonz.ceritaria.studio.core.upload

import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobRepository
import com.flyonz.ceritaria.studio.core.media.TemporaryMediaStore
import com.flyonz.ceritaria.studio.core.media.VideoEncodingStatus
import com.flyonz.ceritaria.studio.core.media.VideoMetadata
import java.io.File
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VideoUploadFailureRetentionTest {
    @Test
    fun finalizeFailureKeepsPreparedOutputForRetry() = runTest {
        val jobs = FakeJobs(encodedJob())
        val store = FakeStore()
        val api = FakeApi(
            target = VideoUploadTarget.Single(
                url = "https://r2/single",
                headers = mapOf("Content-Type" to "video/mp4"),
            ),
            finalizeError = VideoUploadApiException(
                code = "VIDEO_UPLOAD_INTERNAL_ERROR",
                statusCode = 500,
            ),
        )
        val repository = repository(jobs, api, FakeR2(), store)

        runCatching { repository.upload("job-1") }

        assertEquals(VideoUploadStatus.FAILED, jobs.current.uploadStatus)
        assertEquals("VIDEO_UPLOAD_INTERNAL_ERROR", jobs.current.lastErrorCode)
        assertEquals("file:/encoded/job-1.mp4", jobs.current.encodedLocalUri)
        assertNull(store.deletedJobId)
    }

    @Test
    fun multipartPartFailureKeepsCompletedPartForResume() = runTest {
        val jobs = FakeJobs(encodedJob(sizeBytes = 8_000_000))
        val store = FakeStore()
        val codec = MultipartUploadStateCodec()
        val api = FakeApi(
            target = VideoUploadTarget.Multipart(
                partSizeBytes = 5_000_000,
                partCount = 2,
            ),
        )
        val repository = repository(
            jobs = jobs,
            api = api,
            r2 = FakeR2(failOnCall = 2),
            store = store,
            codec = codec,
        )

        runCatching { repository.upload("job-1") }

        assertEquals(VideoUploadStatus.FAILED, jobs.current.uploadStatus)
        assertEquals("R2_HTTP_503", jobs.current.lastErrorCode)
        assertEquals(5_000_000, jobs.current.uploadedBytes)
        val state = codec.decode(jobs.current.multipartState)
        assertNotNull(state)
        assertEquals(listOf(1), state!!.completedParts.map { it.partNumber })
        assertEquals("etag-1", state.completedParts.single().etag)
        assertEquals("file:/encoded/job-1.mp4", jobs.current.encodedLocalUri)
        assertNull(store.deletedJobId)
    }

    private fun repository(
        jobs: FakeJobs,
        api: FakeApi,
        r2: FakeR2,
        store: FakeStore,
        codec: MultipartUploadStateCodec = MultipartUploadStateCodec(),
    ): VideoUploadRepository = VideoUploadRepositoryImpl(
        jobs = jobs,
        api = api,
        sourceResolver = VideoTransferSourceResolver(),
        multipartCodec = codec,
        recovery = VideoUploadRecoveryResolver(api, codec),
        transferRunner = VideoUploadTransferRunner(jobs, api, r2, codec),
        temporaryMediaStore = store,
    )

    private class FakeJobs(initial: VideoJob) : VideoJobRepository {
        private val flow = MutableStateFlow<VideoJob?>(initial)
        var current: VideoJob = initial
            private set

        override suspend fun getById(jobId: String): VideoJob = current
        override fun observeById(jobId: String): Flow<VideoJob?> = flow
        override fun observeLatestForEpisode(episodeId: String): Flow<VideoJob?> = flow

        override suspend fun upsert(job: VideoJob) {
            current = job
            flow.value = job
        }

        override suspend fun delete(jobId: String) = Unit
    }

    private class FakeApi(
        private val target: VideoUploadTarget,
        private val finalizeError: Throwable? = null,
    ) : VideoUploadApi {
        override suspend fun createSession(
            episodeId: String,
            sizeBytes: Long,
        ) = VideoUploadSession(
            assetId = "asset-1",
            sessionId = "session-1",
            expiresAt = "2099-01-01T00:00:00Z",
            target = target,
        )

        override suspend fun authorizePart(
            sessionId: String,
            partNumber: Int,
        ) = VideoUploadPartAuthorization(
            partNumber = partNumber,
            sizeBytes = if (partNumber == 1) 5_000_000 else 3_000_000,
            url = "https://r2/part/" + partNumber,
        )

        override suspend fun completeMultipart(
            sessionId: String,
            parts: List<CompletedVideoPart>,
        ) = Unit

        override suspend fun finalizeUpload(sessionId: String): FinalizedVideoAsset {
            finalizeError?.let { throw it }
            return FinalizedVideoAsset(
                assetId = "asset-1",
                sizeBytes = 8_000_000,
                etag = "final",
            )
        }

        override suspend fun cancelUpload(sessionId: String) = Unit

        override suspend fun getStatus(sessionId: String): RemoteVideoUploadStatus =
            error("unused")
    }

    private class FakeR2(
        private val failOnCall: Int? = null,
    ) : R2UploadDataSource {
        private var callCount = 0

        override suspend fun put(
            url: String,
            headers: Map<String, String>,
            source: VideoTransferSource,
            offsetBytes: Long,
            lengthBytes: Long,
            onProgress: suspend (Long) -> Unit,
        ): R2PutResult {
            callCount += 1
            if (callCount == failOnCall) {
                throw R2UploadException("R2_HTTP_503")
            }
            onProgress(lengthBytes)
            return R2PutResult(etag = "etag-" + callCount)
        }
    }

    private class FakeStore : TemporaryMediaStore {
        var deletedJobId: String? = null

        override fun encodedOutput(jobId: String): File = File(jobId + ".mp4")
        override fun hasEncodedOutput(jobId: String): Boolean = true
        override fun hasCapacity(estimatedOutputBytes: Long): Boolean = true

        override fun deleteEncodedOutput(jobId: String) {
            deletedJobId = jobId
        }
    }

    private fun encodedJob(sizeBytes: Long = 8_000_000) = VideoJob(
        jobId = "job-1",
        episodeId = "episode-1",
        sourceUri = "content://video/source",
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
        encodedLocalUri = "file:/encoded/job-1.mp4",
        encodingStatus = VideoEncodingStatus.READY,
        encodingProgress = 100,
        uploadStatus = VideoUploadStatus.NOT_STARTED,
        uploadedBytes = 0,
        totalBytes = sizeBytes,
        uploadSessionId = null,
        multipartState = null,
        remoteAssetId = null,
        lastErrorCode = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}

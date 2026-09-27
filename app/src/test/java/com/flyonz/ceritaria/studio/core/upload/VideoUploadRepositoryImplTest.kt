package com.flyonz.ceritaria.studio.core.upload

import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobRepository
import com.flyonz.ceritaria.studio.core.media.VideoEncodingStatus
import com.flyonz.ceritaria.studio.core.media.VideoMetadata
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoUploadRepositoryImplTest {
    @Test
    fun singleUploadFinalizesReadyAsset() = runTest {
        val jobs = FakeJobs(job())
        val api = FakeApi(
            createdTarget = VideoUploadTarget.Single(
                url = "https://r2/single",
                headers = mapOf("Content-Type" to "video/mp4"),
            ),
        )
        val r2 = FakeR2()
        val repository = repository(jobs, api, r2)

        val result = repository.upload("job-1")

        assertEquals(VideoUploadStatus.READY, result.uploadStatus)
        assertEquals("asset-ready", result.remoteAssetId)
        assertEquals(8_000_000, result.uploadedBytes)
        assertEquals(listOf(0L to 8_000_000L), r2.ranges)
    }

    @Test
    fun multipartUploadCompletesAllParts() = runTest {
        val jobs = FakeJobs(job(sizeBytes = 8_000_000))
        val api = FakeApi(
            createdTarget = VideoUploadTarget.Multipart(
                partSizeBytes = 5_000_000,
                partCount = 2,
            ),
        )
        val r2 = FakeR2()
        val repository = repository(jobs, api, r2)

        val result = repository.upload("job-1")

        assertEquals(VideoUploadStatus.READY, result.uploadStatus)
        assertEquals(
            listOf(0L to 5_000_000L, 5_000_000L to 3_000_000L),
            r2.ranges,
        )
        assertEquals(listOf(1, 2), api.completedParts.map { it.partNumber })
    }

    @Test
    fun multipartRetrySkipsPersistedCompletedPart() = runTest {
        val codec = MultipartUploadStateCodec()
        val state = MultipartUploadState(
            partSizeBytes = 5_000_000,
            partCount = 2,
            completedParts = listOf(CompletedVideoPart(1, ""etag-old"")),
        )
        val jobs = FakeJobs(
            job(sizeBytes = 8_000_000).copy(
                uploadStatus = VideoUploadStatus.FAILED,
                uploadSessionId = "session-old",
                remoteAssetId = "asset-old",
                multipartState = codec.encode(state),
                uploadedBytes = 5_000_000,
            ),
        )
        val api = FakeApi(
            createdTarget = VideoUploadTarget.Multipart(
                partSizeBytes = 5_000_000,
                partCount = 2,
            ),
            remoteStatus = RemoteVideoUploadStatus(
                sessionId = "session-old",
                assetId = "asset-old",
                mode = "MULTIPART",
                sessionStatus = "UPLOADING",
                assetStatus = "UPLOADING",
                expiresAt = "2099-01-01T00:00:00Z",
                expired = false,
                expectedSizeBytes = 8_000_000,
                actualSizeBytes = null,
                partSizeBytes = 5_000_000,
                partCount = 2,
            ),
        )
        val r2 = FakeR2()
        val repository = repository(jobs, api, r2, codec)

        val result = repository.upload("job-1")

        assertEquals(VideoUploadStatus.READY, result.uploadStatus)
        assertEquals(listOf(5_000_000L to 3_000_000L), r2.ranges)
        assertEquals(listOf(1, 2), api.completedParts.map { it.partNumber })
    }

    private fun repository(
        jobs: FakeJobs,
        api: FakeApi,
        r2: FakeR2,
        codec: MultipartUploadStateCodec = MultipartUploadStateCodec(),
    ): VideoUploadRepository = VideoUploadRepositoryImpl(
        jobs = jobs,
        api = api,
        sourceResolver = VideoTransferSourceResolver(),
        multipartCodec = codec,
        recovery = VideoUploadRecoveryResolver(api, codec),
        transferRunner = VideoUploadTransferRunner(jobs, api, r2, codec),
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
        private val createdTarget: VideoUploadTarget,
        private val remoteStatus: RemoteVideoUploadStatus? = null,
    ) : VideoUploadApi {
        var completedParts: List<CompletedVideoPart> = emptyList()

        override suspend fun createSession(
            episodeId: String,
            sizeBytes: Long,
        ) = VideoUploadSession(
            assetId = "asset-new",
            sessionId = "session-new",
            expiresAt = "2099-01-01T00:00:00Z",
            target = createdTarget,
        )

        override suspend fun authorizePart(
            sessionId: String,
            partNumber: Int,
        ): VideoUploadPartAuthorization {
            val size = if (partNumber == 1) 5_000_000L else 3_000_000L
            return VideoUploadPartAuthorization(
                partNumber = partNumber,
                sizeBytes = size,
                url = "https://r2/part/" + partNumber,
            )
        }

        override suspend fun completeMultipart(
            sessionId: String,
            parts: List<CompletedVideoPart>,
        ) {
            completedParts = parts
        }

        override suspend fun finalizeUpload(sessionId: String) =
            FinalizedVideoAsset(
                assetId = if (sessionId == "session-old") "asset-old" else "asset-ready",
                sizeBytes = 8_000_000,
                etag = ""final"",
            )

        override suspend fun cancelUpload(sessionId: String) = Unit

        override suspend fun getStatus(sessionId: String): RemoteVideoUploadStatus =
            requireNotNull(remoteStatus)
    }

    private class FakeR2 : R2UploadDataSource {
        val ranges = mutableListOf<Pair<Long, Long>>()

        override suspend fun put(
            url: String,
            headers: Map<String, String>,
            source: VideoTransferSource,
            offsetBytes: Long,
            lengthBytes: Long,
            onProgress: suspend (Long) -> Unit,
        ): R2PutResult {
            ranges += offsetBytes to lengthBytes
            onProgress(lengthBytes)
            return R2PutResult(etag = ""etag-" + ranges.size + """)
        }
    }

    private fun job(sizeBytes: Long = 8_000_000) = VideoJob(
        jobId = "job-1",
        episodeId = "episode-1",
        sourceUri = "content://video/source",
        sourceFingerprint = "fingerprint",
        sourceMetadata = VideoMetadata(
            containerMimeType = "video/mp4",
            sizeBytes = sizeBytes,
            durationMs = 30_000,
            width = 1080,
            height = 1920,
            rotationDegrees = 0,
            frameRate = 30f,
            videoMimeType = "video/avc",
            audioMimeType = "audio/mp4a-latm",
            audioBitrate = 128_000,
            audioSampleRate = 48_000,
            hasVideo = true,
            hasAudio = true,
        ),
        needsEncoding = false,
        encodedLocalUri = null,
        encodingStatus = VideoEncodingStatus.NOT_REQUIRED,
        encodingProgress = 0,
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

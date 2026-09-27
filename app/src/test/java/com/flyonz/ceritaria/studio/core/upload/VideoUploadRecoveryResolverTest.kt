package com.flyonz.ceritaria.studio.core.upload

import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob
import com.flyonz.ceritaria.studio.core.media.VideoEncodingStatus
import com.flyonz.ceritaria.studio.core.media.VideoMetadata
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoUploadRecoveryResolverTest {
    private val codec = MultipartUploadStateCodec()

    @Test
    fun unauthenticatedStatusLookupIsPropagated() = runTest {
        val api = FakeApi(
            statusError = VideoUploadApiException("UNAUTHENTICATED", 401),
        )
        val resolver = VideoUploadRecoveryResolver(api, codec)

        val error = runCatching {
            resolver.resolve(jobWithSession())
        }.exceptionOrNull()

        assertTrue(error is VideoUploadApiException)
        assertEquals("UNAUTHENTICATED", (error as VideoUploadApiException).code)
        assertEquals(0, api.cancelCalls)
    }

    @Test
    fun forbiddenStatusLookupIsPropagated() = runTest {
        val api = FakeApi(
            statusError = VideoUploadApiException("FORBIDDEN", 403),
        )
        val resolver = VideoUploadRecoveryResolver(api, codec)

        val error = runCatching {
            resolver.resolve(jobWithSession())
        }.exceptionOrNull()

        assertTrue(error is VideoUploadApiException)
        assertEquals("FORBIDDEN", (error as VideoUploadApiException).code)
        assertEquals(0, api.cancelCalls)
    }

    @Test
    fun serverLookupFailureIsPropagated() = runTest {
        val api = FakeApi(
            statusError = VideoUploadApiException("UPLOAD_SESSION_LOOKUP_FAILED", 500),
        )
        val resolver = VideoUploadRecoveryResolver(api, codec)

        val error = runCatching {
            resolver.resolve(jobWithSession())
        }.exceptionOrNull()

        assertTrue(error is VideoUploadApiException)
        assertEquals(
            "UPLOAD_SESSION_LOOKUP_FAILED",
            (error as VideoUploadApiException).code,
        )
        assertEquals(0, api.cancelCalls)
    }

    @Test
    fun missingRemoteSessionStartsNewSession() = runTest {
        val api = FakeApi(
            statusError = VideoUploadApiException("UPLOAD_SESSION_NOT_FOUND", 404),
        )
        val resolver = VideoUploadRecoveryResolver(api, codec)

        val plan = resolver.resolve(jobWithSession())

        assertTrue(plan is VideoUploadRecoveryPlan.NewSession)
        assertEquals(1, api.cancelCalls)
    }

    @Test
    fun readyRemoteAssetReturnsReadyPlan() = runTest {
        val api = FakeApi(
            status = remoteStatus(
                sessionStatus = "READY",
                assetStatus = "READY",
                actualSizeBytes = 7_500_000,
            ),
        )
        val resolver = VideoUploadRecoveryResolver(api, codec)

        val plan = resolver.resolve(jobWithSession())

        assertEquals(
            VideoUploadRecoveryPlan.Ready("asset-1", 7_500_000),
            plan,
        )
    }

    @Test
    fun incompatibleMultipartStateStartsFreshSession() = runTest {
        val api = FakeApi(
            status = remoteStatus(
                mode = "MULTIPART",
                partSizeBytes = 5_000_000,
                partCount = 2,
            ),
        )
        val resolver = VideoUploadRecoveryResolver(api, codec)
        val job = jobWithSession().copy(
            multipartState = codec.encode(
                MultipartUploadState(
                    partSizeBytes = 6_000_000,
                    partCount = 2,
                    completedParts = emptyList(),
                ),
            ),
        )

        val plan = resolver.resolve(job)

        assertTrue(plan is VideoUploadRecoveryPlan.NewSession)
        assertEquals(1, api.cancelCalls)
    }

    private class FakeApi(
        private val status: RemoteVideoUploadStatus? = null,
        private val statusError: VideoUploadApiException? = null,
    ) : VideoUploadApi {
        var cancelCalls = 0

        override suspend fun createSession(
            episodeId: String,
            sizeBytes: Long,
        ): VideoUploadSession = error("unused")

        override suspend fun authorizePart(
            sessionId: String,
            partNumber: Int,
        ): VideoUploadPartAuthorization = error("unused")

        override suspend fun completeMultipart(
            sessionId: String,
            parts: List<CompletedVideoPart>,
        ) = Unit

        override suspend fun finalizeUpload(
            sessionId: String,
        ): FinalizedVideoAsset = error("unused")

        override suspend fun cancelUpload(sessionId: String) {
            cancelCalls += 1
        }

        override suspend fun getStatus(sessionId: String): RemoteVideoUploadStatus {
            statusError?.let { throw it }
            return requireNotNull(status)
        }
    }

    private fun remoteStatus(
        mode: String = "SINGLE",
        sessionStatus: String = "UPLOADING",
        assetStatus: String = "UPLOADING",
        actualSizeBytes: Long? = null,
        partSizeBytes: Long? = null,
        partCount: Int? = null,
    ) = RemoteVideoUploadStatus(
        sessionId = "session-1",
        assetId = "asset-1",
        mode = mode,
        sessionStatus = sessionStatus,
        assetStatus = assetStatus,
        expiresAt = "2099-01-01T00:00:00Z",
        expired = false,
        expectedSizeBytes = 8_000_000,
        actualSizeBytes = actualSizeBytes,
        partSizeBytes = partSizeBytes,
        partCount = partCount,
    )

    private fun jobWithSession() = VideoJob(
        jobId = "job-1",
        episodeId = "episode-1",
        sourceUri = "content://video/source",
        sourceFingerprint = "fingerprint",
        sourceMetadata = VideoMetadata(
            containerMimeType = "video/mp4",
            sizeBytes = 8_000_000,
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
        uploadStatus = VideoUploadStatus.FAILED,
        uploadedBytes = 0,
        totalBytes = 8_000_000,
        uploadSessionId = "session-1",
        multipartState = null,
        remoteAssetId = "asset-1",
        lastErrorCode = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}

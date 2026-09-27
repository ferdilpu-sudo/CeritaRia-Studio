package com.flyonz.ceritaria.studio.core.upload

import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob
import com.flyonz.ceritaria.studio.core.media.VideoEncodingStatus
import com.flyonz.ceritaria.studio.core.media.VideoMetadata
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoTransferSourceResolverTest {
    private val resolver = VideoTransferSourceResolver()

    @Test
    fun compatibleSourceUsesOriginalUriAndSize() {
        val source = resolver.resolve(
            job(
                needsEncoding = false,
                encodingStatus = VideoEncodingStatus.NOT_REQUIRED,
                encodedLocalUri = null,
                totalBytes = 8_000_000,
            ),
        )

        assertEquals("content://video/source", source.uri)
        assertEquals(8_000_000, source.sizeBytes)
    }

    @Test
    fun encodedSourceUsesPreparedOutput() {
        val source = resolver.resolve(
            job(
                needsEncoding = true,
                encodingStatus = VideoEncodingStatus.READY,
                encodedLocalUri = "file:/encoded/job-1.mp4",
                totalBytes = 5_000_000,
            ),
        )

        assertEquals("file:/encoded/job-1.mp4", source.uri)
        assertEquals(5_000_000, source.sizeBytes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun encodedSourceMustBeReady() {
        resolver.resolve(
            job(
                needsEncoding = true,
                encodingStatus = VideoEncodingStatus.ENCODING,
                encodedLocalUri = null,
                totalBytes = 8_000_000,
            ),
        )
    }

    private fun job(
        needsEncoding: Boolean,
        encodingStatus: VideoEncodingStatus,
        encodedLocalUri: String?,
        totalBytes: Long,
    ) = VideoJob(
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
        needsEncoding = needsEncoding,
        encodedLocalUri = encodedLocalUri,
        encodingStatus = encodingStatus,
        encodingProgress = if (encodingStatus == VideoEncodingStatus.READY) 100 else 0,
        uploadStatus = VideoUploadStatus.NOT_STARTED,
        uploadedBytes = 0,
        totalBytes = totalBytes,
        uploadSessionId = null,
        multipartState = null,
        remoteAssetId = null,
        lastErrorCode = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}

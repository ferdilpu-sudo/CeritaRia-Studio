package com.flyonz.ceritaria.studio.core.database.videojob

import com.flyonz.ceritaria.studio.core.media.VideoEncodingStatus
import com.flyonz.ceritaria.studio.core.media.VideoMetadata
import com.flyonz.ceritaria.studio.core.upload.VideoUploadStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoJobMapperTest {
    @Test
    fun roundTripPreservesOperationalState() {
        val original = VideoJob(
            jobId = "job-1",
            episodeId = "episode-1",
            sourceUri = "content://video/1",
            sourceFingerprint = "fingerprint-1",
            sourceMetadata = VideoMetadata(
                containerMimeType = "video/mp4",
                sizeBytes = 12_345_678,
                durationMs = 60_000,
                width = 1080,
                height = 1920,
                rotationDegrees = 0,
                frameRate = 29.97f,
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
            totalBytes = 12_345_678,
            uploadSessionId = null,
            multipartState = null,
            remoteAssetId = null,
            lastErrorCode = null,
            createdAt = Instant.parse("2026-09-27T08:00:00Z"),
            updatedAt = Instant.parse("2026-09-27T08:05:00Z"),
        )

        val restored = original.toEntity().toDomain()

        assertEquals(original, restored)
    }
}

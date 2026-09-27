package com.flyonz.ceritaria.studio.core.database.videojob

import com.flyonz.ceritaria.studio.core.media.VideoCompatibilityChecker
import com.flyonz.ceritaria.studio.core.media.VideoEncodingStatus
import com.flyonz.ceritaria.studio.core.media.VideoMetadata
import com.flyonz.ceritaria.studio.core.upload.VideoUploadStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoJobFactoryTest {
    private val factory = VideoJobFactory(VideoCompatibilityChecker())

    @Test
    fun compatibleSourceSkipsEncoding() {
        val job = factory.create(
            sourceUri = "content://video/compatible",
            metadata = metadata(),
            jobId = "job-1",
            now = Instant.EPOCH,
        )

        assertFalse(job.needsEncoding)
        assertEquals(VideoEncodingStatus.NOT_REQUIRED, job.encodingStatus)
        assertEquals(VideoUploadStatus.NOT_STARTED, job.uploadStatus)
    }

    @Test
    fun incompatibleSourceQueuesEncoding() {
        val job = factory.create(
            sourceUri = "content://video/hevc",
            metadata = metadata(videoMimeType = "video/hevc"),
            jobId = "job-2",
            now = Instant.EPOCH,
        )

        assertTrue(job.needsEncoding)
        assertEquals(VideoEncodingStatus.QUEUED, job.encodingStatus)
    }

    @Test
    fun fingerprintIsStableForSameSelectionAndMetadata() {
        val first = factory.create(
            sourceUri = "content://video/1",
            metadata = metadata(),
            jobId = "job-1",
            now = Instant.EPOCH,
        )
        val second = factory.create(
            sourceUri = "content://video/1",
            metadata = metadata(),
            jobId = "job-2",
            now = Instant.EPOCH,
        )

        assertEquals(first.sourceFingerprint, second.sourceFingerprint)
    }

    private fun metadata(
        videoMimeType: String = "video/avc",
    ) = VideoMetadata(
        containerMimeType = "video/mp4",
        sizeBytes = 10_000_000,
        durationMs = 30_000,
        width = 1080,
        height = 1920,
        rotationDegrees = 0,
        frameRate = 30f,
        videoMimeType = videoMimeType,
        audioMimeType = "audio/mp4a-latm",
        audioBitrate = 192_000,
        audioSampleRate = 48_000,
        hasVideo = true,
        hasAudio = true,
    )
}

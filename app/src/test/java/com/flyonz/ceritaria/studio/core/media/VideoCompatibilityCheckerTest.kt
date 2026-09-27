package com.flyonz.ceritaria.studio.core.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCompatibilityCheckerTest {
    private val checker = VideoCompatibilityChecker()

    @Test
    fun acceptsVerifiedPortraitAvcAacSource() {
        val result = checker.check(metadata())

        assertTrue(result.isCompatible)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun rotatedDimensionsUseDisplayOrientation() {
        val result = checker.check(
            metadata(
                width = 1920,
                height = 1080,
                rotationDegrees = 90,
            ),
        )

        assertTrue(result.isCompatible)
    }

    @Test
    fun rejectsFrameRateAboveThirty() {
        val result = checker.check(metadata(frameRate = 60f))

        assertFalse(result.isCompatible)
        assertTrue(VideoCompatibilityIssue.FRAME_RATE in result.issues)
    }

    @Test
    fun rejectsUnknownFrameRateBecauseCompatibilityCannotBeProven() {
        val result = checker.check(metadata(frameRate = null))

        assertFalse(result.isCompatible)
        assertTrue(VideoCompatibilityIssue.FRAME_RATE_UNKNOWN in result.issues)
    }

    @Test
    fun higherAacBitrateDoesNotForceLossyReencode() {
        val result = checker.check(metadata(audioBitrate = 192_000))

        assertTrue(result.isCompatible)
    }

    @Test
    fun silentVideoDoesNotRequireAudioCodec() {
        val result = checker.check(
            metadata(
                hasAudio = false,
                audioMimeType = null,
                audioBitrate = null,
            ),
        )

        assertTrue(result.isCompatible)
    }

    @Test
    fun rejectsOversizedOrNonAvcVideo() {
        val result = checker.check(
            metadata(
                width = 1440,
                videoMimeType = "video/hevc",
            ),
        )

        assertFalse(result.isCompatible)
        assertTrue(VideoCompatibilityIssue.RESOLUTION in result.issues)
        assertTrue(VideoCompatibilityIssue.VIDEO_CODEC in result.issues)
    }

    private fun metadata(
        width: Int? = 1080,
        height: Int? = 1920,
        rotationDegrees: Int? = 0,
        frameRate: Float? = 29.97f,
        videoMimeType: String? = "video/avc",
        audioMimeType: String? = "audio/mp4a-latm",
        audioBitrate: Int? = 128_000,
        hasAudio: Boolean = true,
    ) = VideoMetadata(
        containerMimeType = "video/mp4",
        sizeBytes = 20_000_000,
        durationMs = 60_000,
        width = width,
        height = height,
        rotationDegrees = rotationDegrees,
        frameRate = frameRate,
        videoMimeType = videoMimeType,
        audioMimeType = audioMimeType,
        audioBitrate = audioBitrate,
        audioSampleRate = 48_000,
        hasVideo = true,
        hasAudio = hasAudio,
    )
}

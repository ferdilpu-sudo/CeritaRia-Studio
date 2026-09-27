package com.flyonz.ceritaria.studio.core.media

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoCompatibilityChecker @Inject constructor() {
    private val preset = StreamingPreset()

    fun check(metadata: VideoMetadata): VideoCompatibilityResult {
        val issues = buildSet {
            if (!metadata.hasVideo) add(VideoCompatibilityIssue.MISSING_VIDEO)
            if (metadata.containerMimeType != preset.containerMimeType) {
                add(VideoCompatibilityIssue.CONTAINER)
            }
            if (metadata.videoMimeType != preset.videoMimeType) {
                add(VideoCompatibilityIssue.VIDEO_CODEC)
            }
            checkResolution(metadata)?.let(::add)
            checkFrameRate(metadata.frameRate)?.let(::add)
            if (metadata.hasAudio && metadata.audioMimeType != preset.audioMimeType) {
                add(VideoCompatibilityIssue.AUDIO_CODEC)
            }
        }
        return VideoCompatibilityResult(
            isCompatible = issues.isEmpty(),
            issues = issues,
        )
    }

    private fun checkResolution(metadata: VideoMetadata): VideoCompatibilityIssue? {
        val width = metadata.width ?: return VideoCompatibilityIssue.RESOLUTION_UNKNOWN
        val height = metadata.height ?: return VideoCompatibilityIssue.RESOLUTION_UNKNOWN
        val rotation = normalizeRotation(metadata.rotationDegrees ?: 0)
        val displayWidth = if (rotation == 90 || rotation == 270) height else width
        val displayHeight = if (rotation == 90 || rotation == 270) width else height
        return if (displayWidth > preset.maxWidth || displayHeight > preset.maxHeight) {
            VideoCompatibilityIssue.RESOLUTION
        } else {
            null
        }
    }

    private fun checkFrameRate(frameRate: Float?): VideoCompatibilityIssue? = when {
        frameRate == null || frameRate <= 0f -> VideoCompatibilityIssue.FRAME_RATE_UNKNOWN
        frameRate > preset.maxFrameRate -> VideoCompatibilityIssue.FRAME_RATE
        else -> null
    }

    private fun normalizeRotation(rotationDegrees: Int): Int =
        ((rotationDegrees % 360) + 360) % 360
}

data class VideoCompatibilityResult(
    val isCompatible: Boolean,
    val issues: Set<VideoCompatibilityIssue>,
)

enum class VideoCompatibilityIssue {
    MISSING_VIDEO,
    CONTAINER,
    VIDEO_CODEC,
    RESOLUTION_UNKNOWN,
    RESOLUTION,
    FRAME_RATE_UNKNOWN,
    FRAME_RATE,
    AUDIO_CODEC,
}

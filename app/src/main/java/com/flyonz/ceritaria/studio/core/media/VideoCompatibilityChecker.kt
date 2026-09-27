package com.flyonz.ceritaria.studio.core.media

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoCompatibilityChecker @Inject constructor() {
    private val outputPlanner = VideoOutputPlanner()
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
            if (outputPlanner.plan(metadata) == null) {
                add(VideoCompatibilityIssue.RESOLUTION_UNKNOWN)
            } else if (!outputPlanner.fitsPreset(metadata)) {
                add(VideoCompatibilityIssue.RESOLUTION)
            }
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

    private fun checkFrameRate(frameRate: Float?): VideoCompatibilityIssue? = when {
        frameRate == null || frameRate <= 0f -> VideoCompatibilityIssue.FRAME_RATE_UNKNOWN
        frameRate > preset.maxFrameRate -> VideoCompatibilityIssue.FRAME_RATE
        else -> null
    }
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

package com.flyonz.ceritaria.studio.core.media

data class VideoMetadata(
    val containerMimeType: String?,
    val sizeBytes: Long,
    val durationMs: Long?,
    val width: Int?,
    val height: Int?,
    val rotationDegrees: Int?,
    val frameRate: Float?,
    val videoMimeType: String?,
    val audioMimeType: String?,
    val audioBitrate: Int?,
    val audioSampleRate: Int?,
    val hasVideo: Boolean,
    val hasAudio: Boolean,
)

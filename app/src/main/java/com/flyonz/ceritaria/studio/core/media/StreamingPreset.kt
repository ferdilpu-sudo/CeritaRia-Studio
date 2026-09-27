package com.flyonz.ceritaria.studio.core.media

data class StreamingPreset(
    val containerMimeType: String = "video/mp4",
    val videoMimeType: String = "video/avc",
    val audioMimeType: String = "audio/mp4a-latm",
    val maxShortSide: Int = 1080,
    val maxLongSide: Int = 1920,
    val maxFrameRate: Float = 30f,
    val targetAudioBitrate: Int = 128_000,
)

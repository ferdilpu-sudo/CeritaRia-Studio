package com.flyonz.ceritaria.studio.core.upload

data class VideoUploadSession(
    val assetId: String,
    val sessionId: String,
    val expiresAt: String,
    val target: VideoUploadTarget,
)

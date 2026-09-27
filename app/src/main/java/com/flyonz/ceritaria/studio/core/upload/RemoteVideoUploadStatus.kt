package com.flyonz.ceritaria.studio.core.upload

data class RemoteVideoUploadStatus(
    val sessionId: String,
    val assetId: String,
    val mode: String,
    val sessionStatus: String,
    val assetStatus: String,
    val expiresAt: String,
    val expired: Boolean,
    val expectedSizeBytes: Long,
    val actualSizeBytes: Long?,
    val partSizeBytes: Long?,
    val partCount: Int?,
)

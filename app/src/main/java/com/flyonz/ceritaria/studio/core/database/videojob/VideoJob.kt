package com.flyonz.ceritaria.studio.core.database.videojob

import com.flyonz.ceritaria.studio.core.media.VideoEncodingStatus
import com.flyonz.ceritaria.studio.core.media.VideoMetadata
import com.flyonz.ceritaria.studio.core.upload.VideoUploadStatus
import java.time.Instant

data class VideoJob(
    val jobId: String,
    val episodeId: String?,
    val sourceUri: String,
    val sourceFingerprint: String,
    val sourceMetadata: VideoMetadata,
    val needsEncoding: Boolean,
    val encodedLocalUri: String?,
    val encodingStatus: VideoEncodingStatus,
    val encodingProgress: Int,
    val uploadStatus: VideoUploadStatus,
    val uploadedBytes: Long,
    val totalBytes: Long,
    val uploadSessionId: String?,
    val multipartState: String?,
    val remoteAssetId: String?,
    val lastErrorCode: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

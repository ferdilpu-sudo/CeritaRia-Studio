package com.flyonz.ceritaria.studio.core.database.videojob

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "video_jobs",
    indices = [
        Index("episodeId"),
        Index("updatedAtEpochMs"),
    ],
)
data class VideoJobEntity(
    @PrimaryKey val jobId: String,
    val episodeId: String?,
    val sourceUri: String,
    val sourceFingerprint: String,
    val sourceContainerMimeType: String?,
    val sourceSizeBytes: Long,
    val sourceDurationMs: Long?,
    val sourceWidth: Int?,
    val sourceHeight: Int?,
    val sourceRotationDegrees: Int?,
    val sourceFrameRate: Float?,
    val sourceVideoMimeType: String?,
    val sourceAudioMimeType: String?,
    val sourceAudioBitrate: Int?,
    val sourceAudioSampleRate: Int?,
    val sourceHasVideo: Boolean,
    val sourceHasAudio: Boolean,
    val needsEncoding: Boolean,
    val encodedLocalUri: String?,
    val encodingStatus: String,
    val encodingProgress: Int,
    val uploadStatus: String,
    val uploadedBytes: Long,
    val totalBytes: Long,
    val uploadSessionId: String?,
    val multipartState: String?,
    val remoteAssetId: String?,
    val lastErrorCode: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

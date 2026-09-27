package com.flyonz.ceritaria.studio.core.database.videojob

import com.flyonz.ceritaria.studio.core.media.VideoCompatibilityChecker
import com.flyonz.ceritaria.studio.core.media.VideoEncodingStatus
import com.flyonz.ceritaria.studio.core.media.VideoMetadata
import com.flyonz.ceritaria.studio.core.media.VideoSourceFingerprint
import com.flyonz.ceritaria.studio.core.upload.VideoUploadStatus
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class VideoJobFactory @Inject constructor(
    private val compatibilityChecker: VideoCompatibilityChecker,
) {
    fun create(
        sourceUri: String,
        metadata: VideoMetadata,
        episodeId: String? = null,
        jobId: String = UUID.randomUUID().toString(),
        now: Instant = Instant.now(),
    ): VideoJob {
        val compatibility = compatibilityChecker.check(metadata)
        val needsEncoding = !compatibility.isCompatible
        return VideoJob(
            jobId = jobId,
            episodeId = episodeId,
            sourceUri = sourceUri,
            sourceFingerprint = VideoSourceFingerprint.create(sourceUri, metadata),
            sourceMetadata = metadata,
            needsEncoding = needsEncoding,
            encodedLocalUri = null,
            encodingStatus = if (needsEncoding) {
                VideoEncodingStatus.QUEUED
            } else {
                VideoEncodingStatus.NOT_REQUIRED
            },
            encodingProgress = 0,
            uploadStatus = VideoUploadStatus.NOT_STARTED,
            uploadedBytes = 0,
            totalBytes = metadata.sizeBytes,
            uploadSessionId = null,
            multipartState = null,
            remoteAssetId = null,
            lastErrorCode = null,
            createdAt = now,
            updatedAt = now,
        )
    }
}

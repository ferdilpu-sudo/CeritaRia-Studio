package com.flyonz.ceritaria.studio.core.upload

import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobRepository
import java.time.Instant
import javax.inject.Inject

class VideoUploadTransferRunner @Inject constructor(
    private val jobs: VideoJobRepository,
    private val api: VideoUploadApi,
    private val r2: R2UploadDataSource,
    private val multipartCodec: MultipartUploadStateCodec,
) {
    suspend fun uploadSingle(
        job: VideoJob,
        source: VideoTransferSource,
        target: VideoUploadTarget.Single,
        onProgress: suspend (UploadProgress) -> Unit,
    ): VideoJob {
        var current = job.withUploadStatus(VideoUploadStatus.UPLOADING)
        jobs.upsert(current)
        var lastPersisted = current.uploadedBytes

        r2.put(
            url = target.url,
            headers = target.headers,
            source = source,
            offsetBytes = 0L,
            lengthBytes = source.sizeBytes,
        ) { sent ->
            val absolute = sent.coerceIn(0L, source.sizeBytes)
            onProgress(UploadProgress(absolute, source.sizeBytes))
            if (UploadProgressPolicy.shouldPersist(lastPersisted, absolute, source.sizeBytes)) {
                current = current.copy(
                    uploadedBytes = absolute,
                    updatedAt = Instant.now(),
                )
                jobs.upsert(current)
                lastPersisted = absolute
            }
        }

        return finalizeAfterTransfer(
            current.copy(uploadedBytes = source.sizeBytes),
        )
    }

    suspend fun uploadMultipart(
        job: VideoJob,
        source: VideoTransferSource,
        initialState: MultipartUploadState,
        onProgress: suspend (UploadProgress) -> Unit,
    ): VideoJob {
        var state = initialState
        var current = job.withUploadStatus(VideoUploadStatus.UPLOADING)
        jobs.upsert(current)

        for (partNumber in 1..state.partCount) {
            if (state.completedPart(partNumber) != null) continue

            val auth = api.authorizePart(
                sessionId = requireNotNull(current.uploadSessionId),
                partNumber = partNumber,
            )
            val offset = (partNumber - 1L) * state.partSizeBytes
            val completedBefore = state.completedBytes(source.sizeBytes)
            var lastPersisted = current.uploadedBytes

            val result = r2.put(
                url = auth.url,
                headers = emptyMap(),
                source = source,
                offsetBytes = offset,
                lengthBytes = auth.sizeBytes,
            ) { sent ->
                val absolute = (completedBefore + sent).coerceAtMost(source.sizeBytes)
                onProgress(UploadProgress(absolute, source.sizeBytes))
                if (UploadProgressPolicy.shouldPersist(
                        lastPersisted,
                        absolute,
                        source.sizeBytes,
                    )
                ) {
                    current = current.copy(
                        uploadedBytes = absolute,
                        updatedAt = Instant.now(),
                    )
                    jobs.upsert(current)
                    lastPersisted = absolute
                }
            }

            val etag = result.etag?.takeIf(String::isNotBlank)
                ?: throw VideoUploadException(VideoUploadErrorCode.MULTIPART_ETAG_MISSING)
            state = state.withCompleted(CompletedVideoPart(partNumber, etag))
            current = current.copy(
                uploadedBytes = state.completedBytes(source.sizeBytes),
                multipartState = multipartCodec.encode(state),
                updatedAt = Instant.now(),
            )
            jobs.upsert(current)
        }

        return completeAndFinalize(current, state.completedParts)
    }

    suspend fun completeAndFinalize(
        job: VideoJob,
        parts: List<CompletedVideoPart>,
    ): VideoJob {
        api.completeMultipart(
            sessionId = requireNotNull(job.uploadSessionId),
            parts = parts.sortedBy { it.partNumber },
        )
        return finalizeAfterTransfer(job.copy(uploadedBytes = job.totalBytes))
    }

    suspend fun markReady(
        job: VideoJob,
        assetId: String,
        sizeBytes: Long,
    ): VideoJob {
        val ready = job.copy(
            uploadStatus = VideoUploadStatus.READY,
            uploadedBytes = sizeBytes,
            totalBytes = sizeBytes,
            remoteAssetId = assetId,
            multipartState = null,
            lastErrorCode = null,
            updatedAt = Instant.now(),
        )
        jobs.upsert(ready)
        return ready
    }

    private suspend fun finalizeAfterTransfer(job: VideoJob): VideoJob {
        val verifying = job.copy(
            uploadStatus = VideoUploadStatus.VERIFYING,
            updatedAt = Instant.now(),
        )
        jobs.upsert(verifying)
        val asset = api.finalizeUpload(requireNotNull(verifying.uploadSessionId))
        return markReady(verifying, asset.assetId, asset.sizeBytes)
    }

    private fun VideoJob.withUploadStatus(status: VideoUploadStatus): VideoJob = copy(
        uploadStatus = status,
        lastErrorCode = null,
        updatedAt = Instant.now(),
    )
}

package com.flyonz.ceritaria.studio.core.upload

import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class VideoUploadRecoveryResolver @Inject constructor(
    private val api: VideoUploadApi,
    private val multipartCodec: MultipartUploadStateCodec,
) {
    suspend fun resolve(job: VideoJob): VideoUploadRecoveryPlan {
        val sessionId = job.uploadSessionId
            ?: return VideoUploadRecoveryPlan.NewSession

        val remote = try {
            api.getStatus(sessionId)
        } catch (error: VideoUploadApiException) {
            if (error.code !in RESTARTABLE_STATUS_ERRORS) throw error
            cancelBestEffort(sessionId)
            return VideoUploadRecoveryPlan.NewSession
        }

        if (remote.sessionStatus == "READY" || remote.assetStatus == "READY") {
            return VideoUploadRecoveryPlan.Ready(
                assetId = remote.assetId,
                sizeBytes = remote.actualSizeBytes ?: remote.expectedSizeBytes,
            )
        }

        if (remote.expired || remote.sessionStatus in TERMINAL_STATUSES) {
            cancelBestEffort(sessionId)
            return VideoUploadRecoveryPlan.NewSession
        }

        if (remote.mode == "SINGLE") {
            return resolveSingle(sessionId)
        }

        val state = multipartCodec.decode(job.multipartState)
        if (
            state == null ||
            remote.partSizeBytes != state.partSizeBytes ||
            remote.partCount != state.partCount
        ) {
            cancelBestEffort(sessionId)
            return VideoUploadRecoveryPlan.NewSession
        }

        return if (remote.sessionStatus in FINALIZING_STATUSES) {
            VideoUploadRecoveryPlan.CompleteMultipart(state)
        } else {
            VideoUploadRecoveryPlan.ResumeMultipart(state)
        }
    }

    private suspend fun resolveSingle(sessionId: String): VideoUploadRecoveryPlan =
        try {
            val asset = api.finalizeUpload(sessionId)
            VideoUploadRecoveryPlan.Ready(asset.assetId, asset.sizeBytes)
        } catch (error: VideoUploadApiException) {
            if (error.code !in SINGLE_RESTARTABLE_ERRORS) throw error
            cancelBestEffort(sessionId)
            VideoUploadRecoveryPlan.NewSession
        }

    private suspend fun cancelBestEffort(sessionId: String) {
        try {
            api.cancelUpload(sessionId)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            Unit
        }
    }

    private companion object {
        val TERMINAL_STATUSES = setOf("CANCELLED", "FAILED", "EXPIRED")
        val FINALIZING_STATUSES = setOf("COMPLETING", "UPLOADED", "VERIFYING")
        val RESTARTABLE_STATUS_ERRORS = setOf(
            "UPLOAD_SESSION_NOT_FOUND",
            "VIDEO_ASSET_NOT_FOUND",
            "UPLOAD_SESSION_EXPIRED",
        )
        val SINGLE_RESTARTABLE_ERRORS = setOf(
            "UPLOADED_OBJECT_NOT_FOUND",
            "UPLOAD_SESSION_NOT_FINALIZABLE",
            "UPLOAD_SESSION_EXPIRED",
        )
    }
}

sealed interface VideoUploadRecoveryPlan {
    data object NewSession : VideoUploadRecoveryPlan

    data class Ready(
        val assetId: String,
        val sizeBytes: Long,
    ) : VideoUploadRecoveryPlan

    data class ResumeMultipart(
        val state: MultipartUploadState,
    ) : VideoUploadRecoveryPlan

    data class CompleteMultipart(
        val state: MultipartUploadState,
    ) : VideoUploadRecoveryPlan
}

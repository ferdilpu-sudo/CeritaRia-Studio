package com.flyonz.ceritaria.studio.core.upload

import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobRepository
import com.flyonz.ceritaria.studio.core.media.TemporaryMediaStore
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class VideoUploadRepositoryImpl @Inject constructor(
    private val jobs: VideoJobRepository,
    private val api: VideoUploadApi,
    private val sourceResolver: VideoTransferSourceResolver,
    private val multipartCodec: MultipartUploadStateCodec,
    private val recovery: VideoUploadRecoveryResolver,
    private val transferRunner: VideoUploadTransferRunner,
    private val temporaryMediaStore: TemporaryMediaStore,
) : VideoUploadRepository {
    override suspend fun upload(
        jobId: String,
        onProgress: suspend (UploadProgress) -> Unit,
    ): VideoJob {
        val original = jobs.getById(jobId)
            ?: throw VideoUploadException(VideoUploadErrorCode.JOB_NOT_FOUND)
        if (original.uploadStatus == VideoUploadStatus.READY) return original

        val episodeId = original.episodeId
            ?: throw VideoUploadException(VideoUploadErrorCode.EPISODE_NOT_SAVED)
        val source = try {
            sourceResolver.resolve(original)
        } catch (error: IllegalArgumentException) {
            throw VideoUploadException(VideoUploadErrorCode.SOURCE_NOT_READY, error)
        }

        return try {
            val result = resumeOrCreate(
                job = original.copy(totalBytes = source.sizeBytes),
                episodeId = episodeId,
                source = source,
                onProgress = onProgress,
            )
            cleanupLocalOutputIfReady(result)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            markFailed(jobId, error)
            if (error is VideoUploadException) throw error
            throw VideoUploadException(VideoUploadErrorCode.TRANSFER_FAILED, error)
        }
    }

    override suspend fun cancel(jobId: String): VideoJob {
        val job = jobs.getById(jobId)
            ?: throw VideoUploadException(VideoUploadErrorCode.JOB_NOT_FOUND)
        if (job.uploadStatus == VideoUploadStatus.READY) return job

        job.uploadSessionId?.let { api.cancelUpload(it) }
        val cancelled = job.copy(
            uploadStatus = VideoUploadStatus.CANCELLED,
            lastErrorCode = null,
            updatedAt = Instant.now(),
        )
        jobs.upsert(cancelled)
        return cancelled
    }

    private suspend fun resumeOrCreate(
        job: VideoJob,
        episodeId: String,
        source: VideoTransferSource,
        onProgress: suspend (UploadProgress) -> Unit,
    ): VideoJob {
        when (val plan = recovery.resolve(job)) {
            is VideoUploadRecoveryPlan.Ready ->
                return transferRunner.markReady(job, plan.assetId, plan.sizeBytes)
            is VideoUploadRecoveryPlan.ResumeMultipart ->
                return transferRunner.uploadMultipart(job, source, plan.state, onProgress)
            is VideoUploadRecoveryPlan.CompleteMultipart ->
                return transferRunner.completeAndFinalize(job, plan.state.completedParts)
            VideoUploadRecoveryPlan.NewSession -> Unit
        }

        val reset = if (job.uploadSessionId != null) resetLocalSession(job) else job
        val session = api.createSession(episodeId, source.sizeBytes)
        return when (val target = session.target) {
            is VideoUploadTarget.Single -> {
                val current = persistNewSession(reset, session, null)
                transferRunner.uploadSingle(current, source, target, onProgress)
            }
            is VideoUploadTarget.Multipart -> {
                val state = MultipartUploadState(
                    partSizeBytes = target.partSizeBytes,
                    partCount = target.partCount,
                    completedParts = emptyList(),
                )
                val current = persistNewSession(reset, session, state)
                transferRunner.uploadMultipart(current, source, state, onProgress)
            }
        }
    }

    private suspend fun persistNewSession(
        job: VideoJob,
        session: VideoUploadSession,
        state: MultipartUploadState?,
    ): VideoJob {
        val current = job.copy(
            uploadStatus = VideoUploadStatus.UPLOADING,
            uploadedBytes = 0L,
            uploadSessionId = session.sessionId,
            multipartState = state?.let(multipartCodec::encode),
            remoteAssetId = session.assetId,
            lastErrorCode = null,
            updatedAt = Instant.now(),
        )
        jobs.upsert(current)
        return current
    }

    private suspend fun resetLocalSession(job: VideoJob): VideoJob {
        val reset = job.copy(
            uploadStatus = VideoUploadStatus.QUEUED,
            uploadedBytes = 0L,
            uploadSessionId = null,
            multipartState = null,
            remoteAssetId = null,
            lastErrorCode = null,
            updatedAt = Instant.now(),
        )
        jobs.upsert(reset)
        return reset
    }

    private suspend fun cleanupLocalOutputIfReady(job: VideoJob): VideoJob {
        if (job.uploadStatus != VideoUploadStatus.READY || !job.needsEncoding) {
            return job
        }
        temporaryMediaStore.deleteEncodedOutput(job.jobId)
        val cleaned = job.copy(
            encodedLocalUri = null,
            updatedAt = Instant.now(),
        )
        jobs.upsert(cleaned)
        return cleaned
    }

    private suspend fun markFailed(jobId: String, error: Throwable) {
        val current = jobs.getById(jobId) ?: return
        if (current.uploadStatus == VideoUploadStatus.READY) return
        val code = when (error) {
            is VideoUploadApiException -> error.code
            is R2UploadException -> error.code
            is VideoUploadException -> error.code.name
            else -> VideoUploadErrorCode.TRANSFER_FAILED.name
        }
        jobs.upsert(
            current.copy(
                uploadStatus = VideoUploadStatus.FAILED,
                lastErrorCode = code,
                updatedAt = Instant.now(),
            ),
        )
    }
}

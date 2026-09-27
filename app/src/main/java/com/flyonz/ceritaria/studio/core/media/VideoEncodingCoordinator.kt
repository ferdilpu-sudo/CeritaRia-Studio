package com.flyonz.ceritaria.studio.core.media

import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class VideoEncodingCoordinator @Inject constructor(
    private val jobs: VideoJobRepository,
    private val encoder: VideoEncoder,
    private val temporaryMediaStore: TemporaryMediaStore,
) {
    suspend fun encode(jobId: String): VideoJob {
        val original = jobs.getById(jobId)
            ?: throw VideoEncodingException(VideoEncodingErrorCode.JOB_NOT_FOUND)
        if (!original.needsEncoding) {
            throw VideoEncodingException(VideoEncodingErrorCode.ENCODING_NOT_REQUIRED)
        }
        if (!temporaryMediaStore.hasCapacity(original.sourceMetadata.sizeBytes)) {
            val failed = original.transition(
                status = VideoEncodingStatus.FAILED,
                progress = 0,
                errorCode = VideoEncodingErrorCode.INSUFFICIENT_STORAGE.name,
            )
            jobs.upsert(failed)
            throw VideoEncodingException(VideoEncodingErrorCode.INSUFFICIENT_STORAGE)
        }

        val outputFile = temporaryMediaStore.encodedOutput(jobId)
        temporaryMediaStore.deleteEncodedOutput(jobId)
        var current = original.transition(
            status = VideoEncodingStatus.ENCODING,
            progress = 0,
            errorCode = null,
        )
        jobs.upsert(current)

        return try {
            val result = encoder.encode(
                sourceUri = original.sourceUri,
                metadata = original.sourceMetadata,
                outputFile = outputFile,
                onProgress = { progress ->
                    if (progress.percent != current.encodingProgress) {
                        current = current.transition(
                            status = VideoEncodingStatus.ENCODING,
                            progress = progress.percent,
                            errorCode = null,
                        )
                        jobs.upsert(current)
                    }
                },
            )
            val ready = current.copy(
                encodedLocalUri = result.outputUri,
                encodingStatus = VideoEncodingStatus.READY,
                encodingProgress = 100,
                totalBytes = result.sizeBytes,
                lastErrorCode = null,
                updatedAt = Instant.now(),
            )
            jobs.upsert(ready)
            ready
        } catch (error: CancellationException) {
            temporaryMediaStore.deleteEncodedOutput(jobId)
            val cancelled = current.transition(
                status = VideoEncodingStatus.CANCELLED,
                progress = current.encodingProgress,
                errorCode = null,
            )
            jobs.upsert(cancelled)
            throw error
        } catch (error: VideoEncoderException) {
            temporaryMediaStore.deleteEncodedOutput(jobId)
            val code = when (error.code) {
                VideoEncoderErrorCode.UNSUPPORTED_CODEC ->
                    VideoEncodingErrorCode.UNSUPPORTED_CODEC
                VideoEncoderErrorCode.EXPORT_FAILED ->
                    VideoEncodingErrorCode.ENCODER_FAILED
            }
            val failed = current.transition(
                status = VideoEncodingStatus.FAILED,
                progress = current.encodingProgress,
                errorCode = code.name,
            )
            jobs.upsert(failed)
            throw VideoEncodingException(code, error)
        } catch (error: Throwable) {
            temporaryMediaStore.deleteEncodedOutput(jobId)
            val failed = current.transition(
                status = VideoEncodingStatus.FAILED,
                progress = current.encodingProgress,
                errorCode = VideoEncodingErrorCode.ENCODER_FAILED.name,
            )
            jobs.upsert(failed)
            throw VideoEncodingException(VideoEncodingErrorCode.ENCODER_FAILED, error)
        }
    }

    private fun VideoJob.transition(
        status: VideoEncodingStatus,
        progress: Int,
        errorCode: String?,
    ): VideoJob = copy(
        encodingStatus = status,
        encodingProgress = progress.coerceIn(0, 100),
        lastErrorCode = errorCode,
        updatedAt = Instant.now(),
    )
}

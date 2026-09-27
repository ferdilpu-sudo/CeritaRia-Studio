package com.flyonz.ceritaria.studio.core.upload.execution

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.flyonz.ceritaria.studio.core.upload.UploadProgress
import com.flyonz.ceritaria.studio.core.upload.VideoUploadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class VideoUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: VideoUploadRepository,
    private val notificationFactory: VideoUploadNotificationFactory,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID)
            ?.takeIf(String::isNotBlank)
            ?: return Result.failure(workDataOf(KEY_ERROR to ERROR_INVALID_INPUT))

        setForeground(notificationFactory.foregroundInfo(jobId, null))
        var lastPercent = -1

        return try {
            repository.upload(jobId) { progress ->
                setProgress(
                    workDataOf(
                        KEY_UPLOADED_BYTES to progress.uploadedBytes,
                        KEY_TOTAL_BYTES to progress.totalBytes,
                    ),
                )
                if (progress.percent != lastPercent) {
                    lastPercent = progress.percent
                    setForeground(
                        notificationFactory.foregroundInfo(jobId, progress),
                    )
                }
            }
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            if (runAttemptCount < MAX_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure(workDataOf(KEY_ERROR to ERROR_UPLOAD_FAILED))
            }
        }
    }

    companion object {
        const val KEY_JOB_ID = "video_job_id"
        const val KEY_UPLOADED_BYTES = "uploaded_bytes"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_ERROR = "error"

        private const val ERROR_INVALID_INPUT = "INVALID_INPUT"
        private const val ERROR_UPLOAD_FAILED = "UPLOAD_FAILED"
        private const val MAX_ATTEMPTS = 3
    }
}

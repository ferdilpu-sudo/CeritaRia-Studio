package com.flyonz.ceritaria.studio.core.upload.execution

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.annotation.RequiresApi
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.flyonz.ceritaria.studio.core.upload.VideoUploadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidVideoUploadScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: VideoUploadRepository,
) : VideoUploadScheduler {
    private val workManager by lazy { WorkManager.getInstance(context) }

    override fun enqueue(
        jobId: String,
        totalBytes: Long,
    ): VideoUploadScheduleResult =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            scheduleUidt(jobId, totalBytes)
        } else {
            scheduleWorker(jobId)
        }

    override suspend fun cancel(jobId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val scheduler = context.getSystemService(JobScheduler::class.java)
            scheduler.cancel(VideoUploadExecutionIds.systemJobId(jobId))
        } else {
            workManager.cancelUniqueWork(VideoUploadExecutionIds.workName(jobId))
        }
        repository.cancel(jobId)
    }

    private fun scheduleWorker(jobId: String): VideoUploadScheduleResult {
        val request = OneTimeWorkRequestBuilder<VideoUploadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setInputData(
                workDataOf(VideoUploadWorker.KEY_JOB_ID to jobId),
            )
            .build()

        workManager.enqueueUniqueWork(
            VideoUploadExecutionIds.workName(jobId),
            ExistingWorkPolicy.KEEP,
            request,
        )
        return VideoUploadScheduleResult.SCHEDULED
    }

    @RequiresApi(API_UIDT)
    private fun scheduleUidt(
        jobId: String,
        totalBytes: Long,
    ): VideoUploadScheduleResult {
        val scheduler = context.getSystemService(JobScheduler::class.java)
        val systemJobId = VideoUploadExecutionIds.systemJobId(jobId)
        if (scheduler.getPendingJob(systemJobId) != null) {
            return VideoUploadScheduleResult.SCHEDULED
        }

        val extras = PersistableBundle().apply {
            putString(VideoUploadJobService.KEY_JOB_ID, jobId)
        }
        val builder = JobInfo.Builder(
            systemJobId,
            ComponentName(context, VideoUploadJobService::class.java),
        )
            .setUserInitiated(true)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setExtras(extras)

        if (totalBytes > 0L) {
            builder.setEstimatedNetworkBytes(0L, totalBytes)
        }

        return if (scheduler.schedule(builder.build()) == JobScheduler.RESULT_SUCCESS) {
            VideoUploadScheduleResult.SCHEDULED
        } else {
            VideoUploadScheduleResult.REJECTED
        }
    }

    private companion object {
        const val API_UIDT = 34
    }
}

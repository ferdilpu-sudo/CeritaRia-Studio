package com.flyonz.ceritaria.studio.core.upload.execution

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import androidx.annotation.RequiresApi
import com.flyonz.ceritaria.studio.core.coroutines.IoDispatcher
import com.flyonz.ceritaria.studio.core.coroutines.MainDispatcher
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobRepository
import com.flyonz.ceritaria.studio.core.upload.VideoUploadRepository
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@AndroidEntryPoint
class VideoUploadJobService : JobService() {
    @Inject
    lateinit var repository: VideoUploadRepository

    @Inject
    lateinit var jobs: VideoJobRepository

    @Inject
    lateinit var notificationFactory: VideoUploadNotificationFactory

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    private val runningJobs = ConcurrentHashMap<Int, Job>()
    private val scope by lazy {
        CoroutineScope(SupervisorJob() + ioDispatcher)
    }

    override fun onStartJob(params: JobParameters): Boolean {
        val jobId = params.extras.getString(KEY_JOB_ID)
            ?.takeIf(String::isNotBlank)
            ?: return false

        setNotification(
            params,
            VideoUploadExecutionIds.notificationId(jobId),
            notificationFactory.notification(jobId, null),
            JOB_END_NOTIFICATION_POLICY_REMOVE,
        )

        val running = scope.launch {
            var lastPercent = -1
            try {
                repository.upload(jobId) { progress ->
                    if (progress.percent != lastPercent) {
                        lastPercent = progress.percent
                        withContext(mainDispatcher) {
                            setNotification(
                                params,
                                VideoUploadExecutionIds.notificationId(jobId),
                                notificationFactory.notification(jobId, progress),
                                JOB_END_NOTIFICATION_POLICY_REMOVE,
                            )
                        }
                    }
                }
                finish(params, wantsReschedule = false)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                val errorCode = jobs.getById(jobId)?.lastErrorCode
                finish(
                    params,
                    wantsReschedule = errorCode == null ||
                        VideoUploadRetryPolicy.isRetryable(errorCode),
                )
            }
        }
        runningJobs[params.jobId] = running
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        runningJobs.remove(params.jobId)?.cancel()
        return true
    }

    override fun onDestroy() {
        runningJobs.clear()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun finish(
        params: JobParameters,
        wantsReschedule: Boolean,
    ) {
        withContext(mainDispatcher) {
            runningJobs.remove(params.jobId)
            jobFinished(params, wantsReschedule)
        }
    }

    companion object {
        const val KEY_JOB_ID = "video_job_id"
    }
}

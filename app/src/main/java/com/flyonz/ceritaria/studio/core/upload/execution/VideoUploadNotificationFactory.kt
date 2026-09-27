package com.flyonz.ceritaria.studio.core.upload.execution

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.ForegroundInfo
import com.flyonz.ceritaria.studio.R
import com.flyonz.ceritaria.studio.core.upload.UploadProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoUploadNotificationFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun notification(
        jobId: String,
        progress: UploadProgress?,
    ): Notification {
        ensureChannel()
        val percent = progress?.percent
        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(context.getString(R.string.video_upload_notification_title))
            .setContentText(
                if (percent == null) {
                    context.getString(R.string.video_upload_notification_starting)
                } else {
                    context.getString(R.string.video_upload_notification_progress, percent)
                },
            )
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, percent ?: 0, percent == null)
            .build()
    }

    fun foregroundInfo(
        jobId: String,
        progress: UploadProgress?,
    ): ForegroundInfo {
        val id = VideoUploadExecutionIds.notificationId(jobId)
        val notification = notification(jobId, progress)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                id,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(id, notification)
        }
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.video_upload_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    companion object {
        const val CHANNEL_ID = "video_uploads"
    }
}

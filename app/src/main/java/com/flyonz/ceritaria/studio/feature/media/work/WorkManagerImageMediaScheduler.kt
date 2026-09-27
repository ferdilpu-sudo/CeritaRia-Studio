package com.flyonz.ceritaria.studio.feature.media.work

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaSlot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class WorkManagerImageMediaScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ImageMediaScheduler {
    private val workManager by lazy { WorkManager.getInstance(context) }

    override fun replace(
        ownerId: String,
        slot: ImageMediaSlot,
        sourceUri: String,
        oldPublicUrl: String?,
    ): UUID {
        persistReadPermission(sourceUri)
        return enqueue(
            ownerId = ownerId,
            slot = slot,
            action = ACTION_REPLACE,
            sourceUri = sourceUri,
            oldPublicUrl = oldPublicUrl,
        )
    }

    override fun remove(
        ownerId: String,
        slot: ImageMediaSlot,
        oldPublicUrl: String?,
    ): UUID = enqueue(
        ownerId = ownerId,
        slot = slot,
        action = ACTION_REMOVE,
        sourceUri = null,
        oldPublicUrl = oldPublicUrl,
    )

    override fun observe(workId: UUID): Flow<ImageMediaWorkState?> =
        workManager.getWorkInfoByIdFlow(workId).map { info ->
            info?.toMediaState()
        }

    override fun cancel(workId: UUID) {
        workManager.cancelWorkById(workId)
    }

    private fun enqueue(
        ownerId: String,
        slot: ImageMediaSlot,
        action: String,
        sourceUri: String?,
        oldPublicUrl: String?,
    ): UUID {
        val operationId = UUID.randomUUID().toString()
        val request = OneTimeWorkRequestBuilder<ImageMediaWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setInputData(
                workDataOf(
                    ImageMediaWorker.KEY_OWNER_ID to ownerId,
                    ImageMediaWorker.KEY_SLOT to slot.name,
                    ImageMediaWorker.KEY_ACTION to action,
                    ImageMediaWorker.KEY_OPERATION_ID to operationId,
                    ImageMediaWorker.KEY_SOURCE_URI to sourceUri,
                    ImageMediaWorker.KEY_OLD_PUBLIC_URL to oldPublicUrl,
                ),
            )
            .addTag(tag(ownerId, slot))
            .build()

        workManager.enqueueUniqueWork(
            uniqueName(ownerId, slot),
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return request.id
    }

    private fun persistReadPermission(sourceUri: String) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                Uri.parse(sourceUri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    private fun WorkInfo.toMediaState(): ImageMediaWorkState {
        val mappedStatus = when (state) {
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.BLOCKED,
            -> ImageMediaWorkStatus.QUEUED
            WorkInfo.State.RUNNING -> ImageMediaWorkStatus.RUNNING
            WorkInfo.State.SUCCEEDED -> ImageMediaWorkStatus.SUCCEEDED
            WorkInfo.State.FAILED -> ImageMediaWorkStatus.FAILED
            WorkInfo.State.CANCELLED -> ImageMediaWorkStatus.CANCELLED
        }
        return ImageMediaWorkState(
            workId = id,
            status = mappedStatus,
            progress = if (mappedStatus == ImageMediaWorkStatus.SUCCEEDED) {
                100
            } else {
                progress.getInt(ImageMediaWorker.KEY_PROGRESS, 0)
            },
            publicUrl = outputData.getString(ImageMediaWorker.KEY_PUBLIC_URL),
            removed = outputData.getBoolean(ImageMediaWorker.KEY_REMOVED, false),
            errorCode = outputData.getString(ImageMediaWorker.KEY_ERROR_CODE),
        )
    }

    private fun uniqueName(ownerId: String, slot: ImageMediaSlot): String =
        "image-media-$ownerId-${slot.name}"

    private fun tag(ownerId: String, slot: ImageMediaSlot): String =
        "image-media:$ownerId:${slot.name}"

    private companion object {
        const val ACTION_REPLACE = "REPLACE"
        const val ACTION_REMOVE = "REMOVE"
    }
}

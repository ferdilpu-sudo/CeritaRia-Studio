package com.flyonz.ceritaria.studio.feature.media.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaRepository
import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaSlot
import com.flyonz.ceritaria.studio.feature.media.source.ImageSourceException
import com.flyonz.ceritaria.studio.feature.media.source.ImageSourceReader
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ImageMediaWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val sourceReader: ImageSourceReader,
    private val repository: ImageMediaRepository,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val command = commandOrNull() ?: return failure(ERROR_INVALID_INPUT)
        return when (command.action) {
            ImageMediaAction.REPLACE -> replace(command)
            ImageMediaAction.REMOVE -> remove(command)
        }
    }

    private suspend fun replace(command: ImageMediaCommand): Result {
        val sourceUri = command.sourceUri ?: return failure(ERROR_INVALID_INPUT)
        var prepared: com.flyonz.ceritaria.studio.feature.media.source.PreparedImage? = null
        return try {
            setProgress(workDataOf(KEY_PROGRESS to 10))
            prepared = sourceReader.prepare(sourceUri, command.operationId)
            setProgress(workDataOf(KEY_PROGRESS to 45))

            val publicUrl = repository.replace(
                ownerId = command.ownerId,
                slot = command.slot,
                operationId = command.operationId,
                file = prepared.file,
                selection = prepared.selection,
                oldPublicUrl = command.oldPublicUrl,
            )
            setProgress(workDataOf(KEY_PROGRESS to 100))
            Result.success(
                workDataOf(
                    KEY_PUBLIC_URL to publicUrl,
                    KEY_REMOVED to false,
                    KEY_PROGRESS to 100,
                ),
            )
        } catch (error: ImageSourceException) {
            failure(error.issue?.name ?: ERROR_SOURCE)
        } catch (_: IllegalArgumentException) {
            failure(ERROR_CONFIGURATION)
        } catch (_: Throwable) {
            retryOrFail(ERROR_TRANSFER)
        } finally {
            prepared?.file?.delete()
        }
    }

    private suspend fun remove(command: ImageMediaCommand): Result = try {
        setProgress(workDataOf(KEY_PROGRESS to 35))
        repository.remove(
            ownerId = command.ownerId,
            slot = command.slot,
            oldPublicUrl = command.oldPublicUrl,
        )
        Result.success(
            workDataOf(
                KEY_REMOVED to true,
                KEY_PROGRESS to 100,
            ),
        )
    } catch (_: IllegalArgumentException) {
        failure(ERROR_CONFIGURATION)
    } catch (_: Throwable) {
        retryOrFail(ERROR_TRANSFER)
    }

    private fun commandOrNull(): ImageMediaCommand? {
        val ownerId = inputData.getString(KEY_OWNER_ID)?.takeIf(String::isNotBlank) ?: return null
        val slot = inputData.getString(KEY_SLOT)
            ?.let { runCatching { ImageMediaSlot.valueOf(it) }.getOrNull() }
            ?: return null
        val action = inputData.getString(KEY_ACTION)
            ?.let { runCatching { ImageMediaAction.valueOf(it) }.getOrNull() }
            ?: return null
        val operationId = inputData.getString(KEY_OPERATION_ID)
            ?.takeIf(String::isNotBlank)
            ?: id.toString()

        return ImageMediaCommand(
            ownerId = ownerId,
            slot = slot,
            action = action,
            operationId = operationId,
            sourceUri = inputData.getString(KEY_SOURCE_URI),
            oldPublicUrl = inputData.getString(KEY_OLD_PUBLIC_URL),
        )
    }

    private fun retryOrFail(errorCode: String): Result =
        if (runAttemptCount < MAX_RETRIES) Result.retry() else failure(errorCode)

    private fun failure(errorCode: String): Result =
        Result.failure(workDataOf(KEY_ERROR_CODE to errorCode))

    companion object {
        const val KEY_OWNER_ID = "owner_id"
        const val KEY_SLOT = "slot"
        const val KEY_ACTION = "action"
        const val KEY_OPERATION_ID = "operation_id"
        const val KEY_SOURCE_URI = "source_uri"
        const val KEY_OLD_PUBLIC_URL = "old_public_url"
        const val KEY_PROGRESS = "progress"
        const val KEY_PUBLIC_URL = "public_url"
        const val KEY_REMOVED = "removed"
        const val KEY_ERROR_CODE = "error_code"

        const val ERROR_INVALID_INPUT = "INVALID_INPUT"
        const val ERROR_SOURCE = "SOURCE"
        const val ERROR_CONFIGURATION = "CONFIGURATION"
        const val ERROR_TRANSFER = "TRANSFER"

        private const val MAX_RETRIES = 3
    }
}

private data class ImageMediaCommand(
    val ownerId: String,
    val slot: ImageMediaSlot,
    val action: ImageMediaAction,
    val operationId: String,
    val sourceUri: String?,
    val oldPublicUrl: String?,
)

private enum class ImageMediaAction {
    REPLACE,
    REMOVE,
}

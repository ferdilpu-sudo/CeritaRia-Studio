package com.flyonz.ceritaria.studio.feature.media.data

import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaRepository
import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaSlot
import com.flyonz.ceritaria.studio.feature.media.domain.ImageSelection
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class ImageMediaRepositoryImpl @Inject constructor(
    private val storage: ImageStorageDataSource,
    private val references: ImageReferenceDataSource,
) : ImageMediaRepository {
    override suspend fun replace(
        ownerId: String,
        slot: ImageMediaSlot,
        operationId: String,
        file: File,
        selection: ImageSelection,
        oldPublicUrl: String?,
        onUploadProgress: suspend (Int) -> Unit,
    ): String {
        val contract = slot.contract()
        val objectPath = ownerId + "/" + operationId + "." + selection.extension
        val newPublicUrl = storage.upload(
            bucket = contract.bucket,
            path = objectPath,
            file = file,
            onProgress = onUploadProgress,
        )
        try {
            references.updateReference(ownerId, slot, newPublicUrl)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            bestEffortDeleteObject(contract.bucket, objectPath)
            throw error
        }

        if (oldPublicUrl != newPublicUrl) {
            bestEffortDeleteOwnedUrl(contract.bucket, oldPublicUrl)
        }
        return newPublicUrl
    }

    override suspend fun remove(
        ownerId: String,
        slot: ImageMediaSlot,
        oldPublicUrl: String?,
    ) {
        references.updateReference(ownerId, slot, null)
        bestEffortDeleteOwnedUrl(slot.contract().bucket, oldPublicUrl)
    }

    private suspend fun bestEffortDeleteObject(bucket: String, path: String) {
        try {
            storage.deleteObject(bucket, path)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            Unit
        }
    }

    private suspend fun bestEffortDeleteOwnedUrl(
        bucket: String,
        publicUrl: String?,
    ) {
        try {
            storage.deleteOwnedPublicUrl(bucket, publicUrl)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            Unit
        }
    }
}

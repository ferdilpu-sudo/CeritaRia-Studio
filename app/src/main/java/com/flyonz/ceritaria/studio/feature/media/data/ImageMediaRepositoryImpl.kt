package com.flyonz.ceritaria.studio.feature.media.data

import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaRepository
import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaSlot
import com.flyonz.ceritaria.studio.feature.media.domain.ImageSelection
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

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
    ): String {
        val contract = slot.contract()
        val objectPath = "$ownerId/$operationId.${selection.extension}"
        val newPublicUrl = storage.upload(contract.bucket, objectPath, file)
        try {
            references.updateReference(ownerId, slot, newPublicUrl)
        } catch (error: Throwable) {
            runCatching { storage.deleteObject(contract.bucket, objectPath) }
            throw error
        }

        if (oldPublicUrl != newPublicUrl) {
            runCatching {
                storage.deleteOwnedPublicUrl(contract.bucket, oldPublicUrl)
            }
        }
        return newPublicUrl
    }

    override suspend fun remove(
        ownerId: String,
        slot: ImageMediaSlot,
        oldPublicUrl: String?,
    ) {
        references.updateReference(ownerId, slot, null)
        val contract = slot.contract()
        runCatching {
            storage.deleteOwnedPublicUrl(contract.bucket, oldPublicUrl)
        }
    }
}

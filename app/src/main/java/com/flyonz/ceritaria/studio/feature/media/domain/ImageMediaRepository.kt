package com.flyonz.ceritaria.studio.feature.media.domain

import java.io.File

interface ImageMediaRepository {
    suspend fun replace(
        ownerId: String,
        slot: ImageMediaSlot,
        operationId: String,
        file: File,
        selection: ImageSelection,
        oldPublicUrl: String?,
        onUploadProgress: suspend (Int) -> Unit,
    ): String

    suspend fun remove(
        ownerId: String,
        slot: ImageMediaSlot,
        oldPublicUrl: String?,
    )
}

package com.flyonz.ceritaria.studio.feature.media.work

import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaSlot
import java.util.UUID
import kotlinx.coroutines.flow.Flow

interface ImageMediaScheduler {
    fun replace(
        ownerId: String,
        slot: ImageMediaSlot,
        sourceUri: String,
        oldPublicUrl: String?,
    ): UUID

    fun remove(
        ownerId: String,
        slot: ImageMediaSlot,
        oldPublicUrl: String?,
    ): UUID

    fun observe(workId: UUID): Flow<ImageMediaWorkState?>

    fun cancel(workId: UUID)
}

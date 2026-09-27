package com.flyonz.ceritaria.studio.feature.media.data

import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaSlot

interface ImageReferenceDataSource {
    suspend fun updateReference(
        ownerId: String,
        slot: ImageMediaSlot,
        publicUrl: String?,
    )
}

package com.flyonz.ceritaria.studio.feature.media.presentation

import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaSlot
import java.util.UUID

data class ImageMediaUiState(
    val status: ImageMediaUiStatus = ImageMediaUiStatus.IDLE,
    val progress: Int = 0,
    val workId: UUID? = null,
    val lastSourceUri: String? = null,
    val oldPublicUrl: String? = null,
    val lastAction: ImageMediaAction? = null,
    val errorCode: String? = null,
)

enum class ImageMediaUiStatus {
    IDLE,
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    SAVE_FIRST,
}

enum class ImageMediaAction {
    REPLACE,
    REMOVE,
}

sealed interface ImageMediaEffect {
    data class ReferenceUpdated(
        val slot: ImageMediaSlot,
        val publicUrl: String?,
    ) : ImageMediaEffect
}

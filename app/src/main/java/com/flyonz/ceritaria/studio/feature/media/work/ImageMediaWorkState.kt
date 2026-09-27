package com.flyonz.ceritaria.studio.feature.media.work

import java.util.UUID

data class ImageMediaWorkState(
    val workId: UUID,
    val status: ImageMediaWorkStatus,
    val progress: Int,
    val publicUrl: String? = null,
    val removed: Boolean = false,
    val errorCode: String? = null,
)

enum class ImageMediaWorkStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
}

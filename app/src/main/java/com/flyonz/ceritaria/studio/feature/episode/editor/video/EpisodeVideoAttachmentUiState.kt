package com.flyonz.ceritaria.studio.feature.episode.editor.video

data class EpisodeVideoAttachmentUiState(
    val status: EpisodeVideoAttachmentStatus = EpisodeVideoAttachmentStatus.IDLE,
    val assetId: String? = null,
    val errorCode: String? = null,
)

enum class EpisodeVideoAttachmentStatus {
    IDLE,
    ATTACHING,
    ATTACHED,
    FAILED,
}

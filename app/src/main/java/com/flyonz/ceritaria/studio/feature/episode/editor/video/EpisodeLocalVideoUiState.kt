package com.flyonz.ceritaria.studio.feature.episode.editor.video

import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob

data class EpisodeLocalVideoUiState(
    val status: EpisodeLocalVideoStatus = EpisodeLocalVideoStatus.IDLE,
    val job: VideoJob? = null,
    val errorCode: String? = null,
)

enum class EpisodeLocalVideoStatus {
    IDLE,
    SAVE_FIRST,
    INSPECTING,
    READY_WITHOUT_ENCODING,
    READY_TO_ENCODE,
    ENCODING,
    ENCODED_READY,
    FAILED,
    CANCELLED,
}

package com.flyonz.ceritaria.studio.feature.episode.reorder

import com.flyonz.ceritaria.studio.feature.episode.domain.Episode

data class EpisodeReorderUiState(
    val isLoading: Boolean = true,
    val episodes: List<Episode> = emptyList(),
    val originalOrder: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val loadFailed: Boolean = false,
    val saveError: EpisodeReorderSaveError? = null,
    val saveSucceeded: Boolean = false,
) {
    val isDirty: Boolean
        get() = episodes.map(Episode::id) != originalOrder
}

enum class EpisodeReorderSaveError {
    CONFLICT,
    GENERIC,
}

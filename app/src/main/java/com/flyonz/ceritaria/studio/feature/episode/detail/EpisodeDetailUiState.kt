package com.flyonz.ceritaria.studio.feature.episode.detail

import com.flyonz.ceritaria.studio.feature.episode.domain.Episode

data class EpisodeDetailUiState(
    val isLoading: Boolean = true,
    val episode: Episode? = null,
    val hasError: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: Boolean = false,
)

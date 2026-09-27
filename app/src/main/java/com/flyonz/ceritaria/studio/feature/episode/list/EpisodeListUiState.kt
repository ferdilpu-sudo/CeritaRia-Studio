package com.flyonz.ceritaria.studio.feature.episode.list

import com.flyonz.ceritaria.studio.feature.episode.domain.Episode
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeStatusFilter
import com.flyonz.ceritaria.studio.feature.episode.domain.VideoProviderFilter

data class SeriesOption(
    val id: String,
    val title: String,
)

data class EpisodeListUiState(
    val items: List<Episode> = emptyList(),
    val seriesOptions: List<SeriesOption> = emptyList(),
    val query: String = "",
    val seriesId: String? = null,
    val status: EpisodeStatusFilter = EpisodeStatusFilter.ALL,
    val provider: VideoProviderFilter = VideoProviderFilter.ALL,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val nextPage: Int = 0,
    val hasError: Boolean = false,
)

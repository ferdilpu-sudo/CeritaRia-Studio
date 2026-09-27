package com.flyonz.ceritaria.studio.feature.episode.domain

enum class EpisodeStatusFilter {
    ALL,
    PUBLISHED,
    DRAFT,
    UNPUBLISHED,
}

enum class VideoProviderFilter {
    ALL,
    YOUTUBE,
    FACEBOOK,
}

data class EpisodeQuery(
    val page: Int = 0,
    val pageSize: Int = 20,
    val search: String = "",
    val seriesId: String? = null,
    val status: EpisodeStatusFilter = EpisodeStatusFilter.ALL,
    val provider: VideoProviderFilter = VideoProviderFilter.ALL,
)

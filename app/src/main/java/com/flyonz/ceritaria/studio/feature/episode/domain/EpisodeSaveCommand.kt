package com.flyonz.ceritaria.studio.feature.episode.domain

import java.time.Instant

enum class EditableVideoProvider(val rawValue: String) {
    YOUTUBE("youtube"),
    FACEBOOK("facebook"),
}

data class EpisodeSaveCommand(
    val id: String?,
    val seriesId: String,
    val episodeNumber: Int,
    val slug: String,
    val title: String,
    val shortSynopsis: String?,
    val recap: String?,
    val highlights: List<String>,
    val videoProvider: EditableVideoProvider,
    val videoUrl: String,
    val thumbnailUrl: String?,
    val durationSeconds: Int?,
    val isPublished: Boolean,
    val existingPublishedAt: Instant?,
    val seoTitle: String?,
    val seoDescription: String?,
)

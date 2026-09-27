package com.flyonz.ceritaria.studio.feature.episode.domain

import com.flyonz.ceritaria.studio.core.model.PublishStatus
import java.time.Instant

data class Episode(
    val id: String,
    val seriesId: String,
    val seriesTitle: String?,
    val episodeNumber: Int,
    val slug: String,
    val title: String,
    val shortSynopsis: String?,
    val recap: String?,
    val highlights: List<String>,
    val videoProvider: VideoProvider,
    val videoUrl: String?,
    val videoAssetId: String? = null,
    val thumbnailUrl: String?,
    val durationSeconds: Int?,
    val publishStatus: PublishStatus,
    val publishedAt: Instant?,
    val seoTitle: String?,
    val seoDescription: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)

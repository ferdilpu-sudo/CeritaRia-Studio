package com.flyonz.ceritaria.studio.feature.episode.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodeWriteDto(
    val id: String,
    @SerialName("series_id") val seriesId: String,
    @SerialName("episode_number") val episodeNumber: Int,
    val slug: String,
    val title: String,
    @SerialName("short_synopsis") val shortSynopsis: String?,
    val recap: String?,
    val highlights: List<String>,
    @SerialName("video_provider") val videoProvider: String,
    @SerialName("video_url") val videoUrl: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String?,
    @SerialName("duration_seconds") val durationSeconds: Int?,
    @SerialName("is_published") val isPublished: Boolean,
    @SerialName("published_at") val publishedAt: String?,
    @SerialName("seo_title") val seoTitle: String?,
    @SerialName("seo_description") val seoDescription: String?,
)

@Serializable
data class EpisodeDeleteDto(
    @SerialName("deleted_at") val deletedAt: String,
    @SerialName("is_published") val isPublished: Boolean = false,
)

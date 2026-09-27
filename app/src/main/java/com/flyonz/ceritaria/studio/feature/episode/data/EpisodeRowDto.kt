package com.flyonz.ceritaria.studio.feature.episode.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodeSeriesDto(
    val id: String,
    val title: String,
)

@Serializable
data class EpisodeRowDto(
    val id: String,
    @SerialName("series_id") val seriesId: String,
    @SerialName("episode_number") val episodeNumber: Int,
    val slug: String,
    val title: String,
    @SerialName("short_synopsis") val shortSynopsis: String? = null,
    val recap: String? = null,
    val highlights: List<String> = emptyList(),
    @SerialName("video_provider") val videoProvider: String,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("video_asset_id") val videoAssetId: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    @SerialName("is_published") val isPublished: Boolean,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("seo_title") val seoTitle: String? = null,
    @SerialName("seo_description") val seoDescription: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
    val series: EpisodeSeriesDto? = null,
)

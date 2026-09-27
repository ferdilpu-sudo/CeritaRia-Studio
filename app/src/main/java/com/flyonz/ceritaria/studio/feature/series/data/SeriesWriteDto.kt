package com.flyonz.ceritaria.studio.feature.series.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeriesWriteDto(
    val id: String,
    val slug: String,
    val title: String,
    @SerialName("short_synopsis") val shortSynopsis: String?,
    val synopsis: String?,
    val genres: List<String>,
    @SerialName("cover_url") val coverUrl: String?,
    @SerialName("hero_url") val heroUrl: String?,
    @SerialName("is_featured") val isFeatured: Boolean,
    @SerialName("is_published") val isPublished: Boolean,
    @SerialName("published_at") val publishedAt: String?,
    @SerialName("seo_title") val seoTitle: String?,
    @SerialName("seo_description") val seoDescription: String?,
)

@Serializable
data class SoftDeleteSeriesParams(
    @SerialName("target_id") val targetId: String,
)

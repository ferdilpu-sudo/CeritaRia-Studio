package com.flyonz.ceritaria.studio.feature.series.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeriesRowDto(
    val id: String,
    val slug: String,
    val title: String,
    @SerialName("short_synopsis") val shortSynopsis: String? = null,
    val synopsis: String? = null,
    val genres: List<String> = emptyList(),
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("hero_url") val heroUrl: String? = null,
    @SerialName("is_featured") val isFeatured: Boolean,
    @SerialName("is_published") val isPublished: Boolean,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("seo_title") val seoTitle: String? = null,
    @SerialName("seo_description") val seoDescription: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
)
